"""Расчёт ожидаемых затрат по ФОТ за месяц по сотруднику и в целом."""
from __future__ import annotations

from dataclasses import dataclass, field
from decimal import ROUND_HALF_UP, Decimal
from typing import Iterable

from .models import Employee, PremiumType
from .production_calendar import ProductionCalendar

AVG_MONTH_CALENDAR_DAYS = Decimal("29.3")  # ст. 139 ТК РФ / методика Росстата
CENT = Decimal("0.01")


def _round(value: Decimal) -> Decimal:
    return value.quantize(CENT, rounding=ROUND_HALF_UP)


@dataclass
class EmployeeResult:
    """Разбивка ожидаемых затрат по одному сотруднику за месяц."""

    employee: Employee
    norm_hours: float
    worked_hours: float
    vacation_calendar_days: int
    base_salary: Decimal
    vacation_pay: Decimal
    premium: Decimal
    insurance_contributions: Decimal = Decimal("0")

    @property
    def total(self) -> Decimal:
        return self.base_salary + self.vacation_pay + self.premium

    @property
    def total_with_contributions(self) -> Decimal:
        return self.total + self.insurance_contributions


@dataclass
class PayrollReport:
    """Итоговый отчёт по ФОТ за месяц."""

    year: int
    month: int
    results: list[EmployeeResult] = field(default_factory=list)
    insurance_rate: Decimal = Decimal("0")

    @property
    def total_base_salary(self) -> Decimal:
        return sum((r.base_salary for r in self.results), Decimal("0"))

    @property
    def total_vacation_pay(self) -> Decimal:
        return sum((r.vacation_pay for r in self.results), Decimal("0"))

    @property
    def total_premium(self) -> Decimal:
        return sum((r.premium for r in self.results), Decimal("0"))

    @property
    def total_insurance_contributions(self) -> Decimal:
        return sum((r.insurance_contributions for r in self.results), Decimal("0"))

    @property
    def total_fot(self) -> Decimal:
        return sum((r.total for r in self.results), Decimal("0"))

    @property
    def total_fot_with_contributions(self) -> Decimal:
        return sum((r.total_with_contributions for r in self.results), Decimal("0"))


class PayrollCalculator:
    """Считает ожидаемые затраты по ФОТ за месяц с учётом отпусков,
    нормы часов и премий."""

    def __init__(self, calendar_: ProductionCalendar | None = None):
        self.calendar = calendar_ or ProductionCalendar()

    def calculate_employee(
        self, employee: Employee, year: int, month: int
    ) -> EmployeeResult:
        month_norm = self.calendar.month_norm(year, month)
        norm_hours = month_norm.norm_hours

        vacation_calendar_days = 0
        vacation_missed_hours = 0.0
        for vacation in employee.vacations:
            clipped = vacation.clip_to_month(year, month)
            if clipped is None:
                continue
            start, end = clipped
            vacation_calendar_days += self.calendar.count_calendar_days_excluding_holidays(
                start, end
            )
            vacation_missed_hours += sum(
                1 for d in month_norm.working_days if start <= d <= end
            ) * 8

        worked_hours = max(norm_hours - vacation_missed_hours, 0.0)

        if norm_hours > 0:
            base_salary = _round(
                employee.oklad * employee.rate * Decimal(worked_hours) / Decimal(norm_hours)
            )
        else:
            base_salary = Decimal("0")

        avg_daily = employee.avg_daily_earning
        if avg_daily is None:
            avg_daily = _round(employee.oklad * employee.rate / AVG_MONTH_CALENDAR_DAYS)
        vacation_pay = _round(avg_daily * vacation_calendar_days)

        premium = self._calculate_premium(employee, base_salary)

        return EmployeeResult(
            employee=employee,
            norm_hours=norm_hours,
            worked_hours=worked_hours,
            vacation_calendar_days=vacation_calendar_days,
            base_salary=base_salary,
            vacation_pay=vacation_pay,
            premium=premium,
        )

    def _calculate_premium(self, employee: Employee, base_salary: Decimal) -> Decimal:
        if employee.premium_type == PremiumType.FIXED:
            return _round(employee.premium_value)
        if employee.premium_type == PremiumType.PERCENT_OKLAD:
            return _round(
                employee.oklad * employee.rate * employee.premium_value / Decimal(100)
            )
        if employee.premium_type == PremiumType.PERCENT_WORKED:
            return _round(base_salary * employee.premium_value / Decimal(100))
        raise ValueError(f"Неизвестный тип премии: {employee.premium_type}")

    def calculate_month(
        self,
        employees: Iterable[Employee],
        year: int,
        month: int,
        insurance_rate: Decimal = Decimal("0"),
    ) -> PayrollReport:
        results = []
        for employee in employees:
            result = self.calculate_employee(employee, year, month)
            if insurance_rate:
                result.insurance_contributions = _round(result.total * insurance_rate / Decimal(100))
            results.append(result)
        return PayrollReport(
            year=year, month=month, results=results, insurance_rate=insurance_rate
        )
