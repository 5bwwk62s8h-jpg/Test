from datetime import date

from payroll_calculator.production_calendar import ProductionCalendar


def test_august_2026_norm_no_holidays():
    cal = ProductionCalendar()
    norm = cal.month_norm(2026, 8)
    # Август 2026: 31 календарный день, начинается в субботу -> 21 рабочий день.
    assert norm.calendar_days_count == 31
    assert norm.norm_days == 21
    assert norm.norm_hours == 21 * 8


def test_january_2026_norm_has_new_year_holidays():
    cal = ProductionCalendar()
    norm = cal.month_norm(2026, 1)
    # 1-8 января - нерабочие праздничные дни.
    assert date(2026, 1, 1) not in norm.working_days
    assert date(2026, 1, 8) not in norm.working_days
    assert norm.norm_days < 23  # меньше, чем если бы не было праздников


def test_preholiday_shortens_norm_hours():
    cal = ProductionCalendar()
    # 2026-02-20 (пятница) - предпраздничный рабочий день перед 23 февраля.
    norm = cal.month_norm(2026, 2)
    assert date(2026, 2, 20) in norm.working_days
    assert norm.norm_hours == norm.norm_days * 8 - 1


def test_is_working_day_weekend():
    cal = ProductionCalendar()
    assert cal.is_working_day(date(2026, 8, 15)) is False  # суббота
    assert cal.is_working_day(date(2026, 8, 17)) is True  # понедельник
