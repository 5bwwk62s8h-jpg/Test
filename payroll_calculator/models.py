"""Модели данных: сотрудник, период отпуска."""
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal
from enum import Enum


class PremiumType(str, Enum):
    """Способ расчёта премии."""

    FIXED = "fixed"  # фиксированная сумма
    PERCENT_OKLAD = "percent_oklad"  # % от полного оклада (пропорционально ставке)
    PERCENT_WORKED = "percent_worked"  # % от начисления за фактически отработанное время


@dataclass(frozen=True)
class VacationPeriod:
    """Период отпуска сотрудника (включительно)."""

    start: date
    end: date

    def __post_init__(self) -> None:
        if self.end < self.start:
            raise ValueError(
                f"Дата окончания отпуска {self.end} раньше даты начала {self.start}"
            )

    def overlaps_month(self, year: int, month: int) -> bool:
        month_start = date(year, month, 1)
        next_month = month + 1
        next_year = year
        if next_month > 12:
            next_month = 1
            next_year += 1
        month_end = date(next_year, next_month, 1)
        return self.start < month_end and self.end >= month_start

    def clip_to_month(self, year: int, month: int) -> tuple[date, date] | None:
        """Пересечение периода отпуска с месяцем (year, month), либо None."""
        if not self.overlaps_month(year, month):
            return None
        month_start = date(year, month, 1)
        next_month = month + 1
        next_year = year
        if next_month > 12:
            next_month = 1
            next_year += 1
        month_end_incl = date(next_year, next_month, 1)
        from datetime import timedelta

        month_end_incl -= timedelta(days=1)
        return (max(self.start, month_start), min(self.end, month_end_incl))


@dataclass
class Employee:
    """Сотрудник и параметры для расчёта затрат по ФОТ за месяц."""

    id: str
    name: str
    oklad: Decimal  # оклад за полную норму при ставке 1.0
    rate: Decimal = Decimal("1")  # ставка (0.5, 1.0, 1.5 ...)
    vacations: list[VacationPeriod] = field(default_factory=list)
    premium_type: PremiumType = PremiumType.FIXED
    premium_value: Decimal = Decimal("0")
    avg_daily_earning: Decimal | None = None  # для расчёта отпускных; если не
    # задано — используется упрощённая оценка oklad*rate/29.3 (среднемесячное
    # число календарных дней по методике Росстата/ст.139 ТК РФ)

    def __post_init__(self) -> None:
        if self.oklad < 0:
            raise ValueError(f"Оклад не может быть отрицательным: {self.oklad}")
        if self.rate <= 0:
            raise ValueError(f"Ставка должна быть положительной: {self.rate}")
