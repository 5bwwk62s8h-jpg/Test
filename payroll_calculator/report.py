"""Форматирование отчёта по ФОТ: текстовая таблица и CSV."""
from __future__ import annotations

import csv
from pathlib import Path

from .calculator import PayrollReport

MONTH_NAMES_RU = [
    "", "январь", "февраль", "март", "апрель", "май", "июнь",
    "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
]

_HEADERS = [
    "Сотрудник",
    "Норма, ч",
    "Отработано, ч",
    "Дней отпуска",
    "Оклад за отраб. время",
    "Отпускные",
    "Премия",
    "Страх. взносы",
    "Итого",
]


def _row(result) -> list[str]:
    return [
        result.employee.name,
        f"{result.norm_hours:g}",
        f"{result.worked_hours:g}",
        str(result.vacation_calendar_days),
        f"{result.base_salary:.2f}",
        f"{result.vacation_pay:.2f}",
        f"{result.premium:.2f}",
        f"{result.insurance_contributions:.2f}",
        f"{result.total_with_contributions:.2f}",
    ]


def format_text_report(report: PayrollReport) -> str:
    """Форматирует отчёт в виде текстовой таблицы для консоли."""
    rows = [_row(r) for r in report.results]
    widths = [
        max(len(_HEADERS[i]), *(len(row[i]) for row in rows)) if rows else len(_HEADERS[i])
        for i in range(len(_HEADERS))
    ]

    def fmt_row(cells: list[str]) -> str:
        return " | ".join(cell.ljust(widths[i]) for i, cell in enumerate(cells))

    lines = []
    title = f"Ожидаемые затраты по ФОТ за {MONTH_NAMES_RU[report.month]} {report.year}"
    lines.append(title)
    lines.append("=" * len(title))
    lines.append(fmt_row(_HEADERS))
    lines.append("-+-".join("-" * w for w in widths))
    for row in rows:
        lines.append(fmt_row(row))
    lines.append("")
    lines.append(f"Оклады за отработанное время: {report.total_base_salary:.2f}")
    lines.append(f"Отпускные:                    {report.total_vacation_pay:.2f}")
    lines.append(f"Премии:                       {report.total_premium:.2f}")
    if report.insurance_rate:
        lines.append(f"Страховые взносы ({report.insurance_rate}%):        {report.total_insurance_contributions:.2f}")
        lines.append(f"ИТОГО ФОТ (со взносами):      {report.total_fot_with_contributions:.2f}")
    else:
        lines.append(f"ИТОГО ФОТ:                     {report.total_fot:.2f}")
    return "\n".join(lines)


def write_csv_report(report: PayrollReport, path: str | Path) -> None:
    """Сохраняет постатейный отчёт в CSV."""
    with open(path, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.writer(f, delimiter=";")
        writer.writerow(_HEADERS)
        for result in report.results:
            writer.writerow(_row(result))
        writer.writerow([])
        writer.writerow(["Итого оклады", f"{report.total_base_salary:.2f}"])
        writer.writerow(["Итого отпускные", f"{report.total_vacation_pay:.2f}"])
        writer.writerow(["Итого премии", f"{report.total_premium:.2f}"])
        if report.insurance_rate:
            writer.writerow(["Итого страх. взносы", f"{report.total_insurance_contributions:.2f}"])
            writer.writerow(["ИТОГО ФОТ (со взносами)", f"{report.total_fot_with_contributions:.2f}"])
        else:
            writer.writerow(["ИТОГО ФОТ", f"{report.total_fot:.2f}"])
