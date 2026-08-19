"""Норма рабочего времени за месяц (упрощённый производственный календарь).

Правила:
  - рабочая неделя: понедельник-пятница, 8 часов в день (40-часовая неделя);
  - нерабочие праздничные дни (ст. 112 ТК РФ) исключаются из нормы;
  - предпраздничный рабочий день короче на 1 час.

Точный официальный производственный календарь ежегодно утверждается
с учётом переноса выходных дней постановлением Правительства РФ. Этот
модуль использует упрощённый список дат из data/holidays_ru.json —
при необходимости передайте собственный файл с датами через
ProductionCalendar(holidays_path=...).
"""
from __future__ import annotations

import calendar
import json
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Iterable

HOURS_PER_WORKDAY = 8
DEFAULT_HOLIDAYS_PATH = Path(__file__).parent / "data" / "holidays_ru.json"


@dataclass(frozen=True)
class MonthNorm:
    """Норма времени за месяц и вспомогательные календарные данные."""

    year: int
    month: int
    calendar_days: list[date]
    working_days: list[date]
    norm_hours: float

    @property
    def norm_days(self) -> int:
        return len(self.working_days)

    @property
    def calendar_days_count(self) -> int:
        return len(self.calendar_days)


class ProductionCalendar:
    """Загружает праздники/предпраздничные дни и считает норму часов за месяц."""

    def __init__(self, holidays_path: str | Path | None = None):
        path = Path(holidays_path) if holidays_path else DEFAULT_HOLIDAYS_PATH
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        self.holidays: set[date] = {
            date.fromisoformat(d) for d in data.get("holidays", [])
        }
        self.preholidays: set[date] = {
            date.fromisoformat(d) for d in data.get("preholidays", [])
        }

    def is_holiday(self, d: date) -> bool:
        return d in self.holidays

    def is_working_day(self, d: date) -> bool:
        return d.weekday() < 5 and d not in self.holidays

    def month_calendar_days(self, year: int, month: int) -> list[date]:
        n_days = calendar.monthrange(year, month)[1]
        return [date(year, month, day) for day in range(1, n_days + 1)]

    def month_norm(self, year: int, month: int) -> MonthNorm:
        calendar_days = self.month_calendar_days(year, month)
        working_days = [d for d in calendar_days if self.is_working_day(d)]
        preholiday_workdays = sum(
            1 for d in working_days if d in self.preholidays
        )
        norm_hours = len(working_days) * HOURS_PER_WORKDAY - preholiday_workdays
        return MonthNorm(
            year=year,
            month=month,
            calendar_days=calendar_days,
            working_days=working_days,
            norm_hours=float(norm_hours),
        )

    def count_working_days_in_range(
        self, days: Iterable[date], start: date, end: date
    ) -> int:
        """Кол-во рабочих дней из `days`, попадающих в диапазон [start, end]."""
        return sum(1 for d in days if start <= d <= end)

    def count_calendar_days_excluding_holidays(self, start: date, end: date) -> int:
        """Календарные дни диапазона [start, end] без нерабочих праздничных дней.

        Используется для отпускных: нерабочие праздничные дни, попадающие на
        период отпуска, не оплачиваются и не входят в число дней отпуска
        (ст. 120 ТК РФ).
        """
        count = 0
        current = start
        while current <= end:
            if current not in self.holidays:
                count += 1
            current = date.fromordinal(current.toordinal() + 1)
        return count
