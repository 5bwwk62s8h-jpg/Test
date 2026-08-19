from datetime import date
from decimal import Decimal

from payroll_calculator.calculator import PayrollCalculator
from payroll_calculator.models import Employee, PremiumType, VacationPeriod
from payroll_calculator.production_calendar import ProductionCalendar

YEAR, MONTH = 2026, 8  # 21 рабочий день, 168 часов нормы, без праздников


def make_calculator() -> PayrollCalculator:
    return PayrollCalculator(calendar_=ProductionCalendar())


def test_full_month_no_vacation_no_premium():
    employee = Employee(id="1", name="Тест", oklad=Decimal("100000"))
    result = make_calculator().calculate_employee(employee, YEAR, MONTH)
    assert result.base_salary == Decimal("100000.00")
    assert result.vacation_pay == Decimal("0.00")
    assert result.premium == Decimal("0.00")
    assert result.total == Decimal("100000.00")


def test_part_time_rate_scales_salary():
    employee = Employee(id="2", name="Тест 0.5", oklad=Decimal("100000"), rate=Decimal("0.5"))
    result = make_calculator().calculate_employee(employee, YEAR, MONTH)
    assert result.base_salary == Decimal("50000.00")


def test_full_month_vacation_zero_base_salary_with_vacation_pay():
    employee = Employee(
        id="3",
        name="В отпуске весь месяц",
        oklad=Decimal("150000"),
        vacations=[VacationPeriod(start=date(2026, 8, 1), end=date(2026, 8, 31))],
        avg_daily_earning=Decimal("5000"),
    )
    result = make_calculator().calculate_employee(employee, YEAR, MONTH)
    assert result.base_salary == Decimal("0.00")
    assert result.vacation_calendar_days == 31
    assert result.vacation_pay == Decimal("155000.00")


def test_partial_vacation_reduces_base_salary_proportionally():
    employee = Employee(
        id="4",
        name="Частичный отпуск",
        oklad=Decimal("210000"),  # 210000 / 168ч = 1250 руб/час
        vacations=[VacationPeriod(start=date(2026, 8, 10), end=date(2026, 8, 23))],
        avg_daily_earning=Decimal("4000"),
    )
    result = make_calculator().calculate_employee(employee, YEAR, MONTH)
    # 10-23 августа: рабочих дней внутри периода - пн-пт с 10 по 21 = 10 дней (10,11,12,13,14,17,18,19,20,21)
    missed_hours = result.norm_hours - result.worked_hours
    assert missed_hours == 10 * 8
    assert result.worked_hours == 168 - 80
    expected_base = (Decimal("210000") * Decimal(88) / Decimal(168)).quantize(Decimal("0.01"))
    assert result.base_salary == expected_base
    assert result.vacation_calendar_days == 14  # 10..23 включительно


def test_premium_fixed():
    employee = Employee(
        id="5", name="Премия фикс", oklad=Decimal("100000"),
        premium_type=PremiumType.FIXED, premium_value=Decimal("15000"),
    )
    result = make_calculator().calculate_employee(employee, YEAR, MONTH)
    assert result.premium == Decimal("15000.00")
    assert result.total == Decimal("115000.00")


def test_premium_percent_oklad():
    employee = Employee(
        id="6", name="Премия % от оклада", oklad=Decimal("100000"),
        premium_type=PremiumType.PERCENT_OKLAD, premium_value=Decimal("20"),
    )
    result = make_calculator().calculate_employee(employee, YEAR, MONTH)
    assert result.premium == Decimal("20000.00")


def test_premium_percent_worked_scales_with_vacation():
    employee = Employee(
        id="7", name="Премия % от отраб.", oklad=Decimal("168000"),  # 1000 руб/час
        vacations=[VacationPeriod(start=date(2026, 8, 1), end=date(2026, 8, 31))],
        premium_type=PremiumType.PERCENT_WORKED, premium_value=Decimal("10"),
        avg_daily_earning=Decimal("0"),
    )
    result = make_calculator().calculate_employee(employee, YEAR, MONTH)
    assert result.base_salary == Decimal("0.00")
    assert result.premium == Decimal("0.00")  # 10% от нулевой базы


def test_insurance_contributions_applied_on_month_total():
    employees = [
        Employee(id="1", name="A", oklad=Decimal("100000")),
        Employee(id="2", name="B", oklad=Decimal("50000")),
    ]
    report = make_calculator().calculate_month(
        employees, YEAR, MONTH, insurance_rate=Decimal("30")
    )
    assert report.total_fot == Decimal("150000.00")
    assert report.total_insurance_contributions == Decimal("45000.00")
    assert report.total_fot_with_contributions == Decimal("195000.00")


def test_default_avg_daily_earning_used_when_not_provided():
    employee = Employee(
        id="8",
        name="Без указания среднего заработка",
        oklad=Decimal("29300"),  # ровно 1000 руб/календарный день по формуле по умолчанию
        vacations=[VacationPeriod(start=date(2026, 8, 1), end=date(2026, 8, 5))],
    )
    result = make_calculator().calculate_employee(employee, YEAR, MONTH)
    assert result.vacation_pay == Decimal("5000.00")
