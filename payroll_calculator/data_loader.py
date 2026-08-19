"""Загрузка списка сотрудников из JSON."""
from __future__ import annotations

import json
from datetime import date
from decimal import Decimal
from pathlib import Path

from .models import Employee, PremiumType, VacationPeriod


def _parse_employee(raw: dict) -> Employee:
    vacations = [
        VacationPeriod(
            start=date.fromisoformat(v["start"]), end=date.fromisoformat(v["end"])
        )
        for v in raw.get("vacations", [])
    ]
    avg_daily = raw.get("avg_daily_earning")
    return Employee(
        id=str(raw["id"]),
        name=raw["name"],
        oklad=Decimal(str(raw["oklad"])),
        rate=Decimal(str(raw.get("rate", 1))),
        vacations=vacations,
        premium_type=PremiumType(raw.get("premium_type", "fixed")),
        premium_value=Decimal(str(raw.get("premium_value", 0))),
        avg_daily_earning=Decimal(str(avg_daily)) if avg_daily is not None else None,
    )


def load_employees(path: str | Path) -> list[Employee]:
    """Загружает список сотрудников из JSON-файла.

    Формат файла: список объектов, см. examples/employees.json.
    """
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    return [_parse_employee(raw) for raw in data]
