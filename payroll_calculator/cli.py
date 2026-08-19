"""CLI: расчёт ожидаемых затрат по ФОТ за месяц.

Пример:
    python -m payroll_calculator.cli --employees examples/employees.json
    python -m payroll_calculator.cli --employees examples/employees.json \\
        --year 2026 --month 8 --insurance-rate 30.2 --csv report.csv
"""
from __future__ import annotations

import argparse
from datetime import date
from decimal import Decimal

from .calculator import PayrollCalculator
from .data_loader import load_employees
from .production_calendar import ProductionCalendar
from .report import format_text_report, write_csv_report


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Расчёт ожидаемых затрат по ФОТ за месяц с учётом отпусков, "
        "нормы часов и премий сотрудников."
    )
    parser.add_argument(
        "--employees",
        required=True,
        help="Путь к JSON-файлу со списком сотрудников (см. examples/employees.json)",
    )
    today = date.today()
    parser.add_argument("--year", type=int, default=today.year, help="Год (по умолчанию — текущий)")
    parser.add_argument(
        "--month", type=int, default=today.month, help="Месяц 1-12 (по умолчанию — текущий)"
    )
    parser.add_argument(
        "--insurance-rate",
        type=str,
        default="0",
        help="Ставка страховых взносов работодателя в %% (например 30.2), по умолчанию 0",
    )
    parser.add_argument(
        "--holidays",
        default=None,
        help="Путь к своему JSON-файлу с праздничными/предпраздничными днями",
    )
    parser.add_argument("--csv", default=None, help="Сохранить отчёт также в CSV-файл")
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_arg_parser()
    args = parser.parse_args(argv)

    if not 1 <= args.month <= 12:
        parser.error("--month должен быть в диапазоне 1-12")

    employees = load_employees(args.employees)
    calendar_ = ProductionCalendar(holidays_path=args.holidays)
    calculator = PayrollCalculator(calendar_=calendar_)
    report = calculator.calculate_month(
        employees,
        year=args.year,
        month=args.month,
        insurance_rate=Decimal(args.insurance_rate),
    )

    print(format_text_report(report))

    if args.csv:
        write_csv_report(report, args.csv)
        print(f"\nCSV-отчёт сохранён: {args.csv}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
