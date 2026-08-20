package com.payroll.calculator.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private const val HOURS_PER_WORKDAY = 8

/** Норма времени за месяц и вспомогательные календарные данные. */
data class MonthNorm(
    val year: Int,
    val month: Int,
    val calendarDays: List<LocalDate>,
    val workingDays: List<LocalDate>,
    val normHours: Double,
) {
    val normDays: Int get() = workingDays.size
    val calendarDaysCount: Int get() = calendarDays.size
}

/**
 * Норма рабочего времени за месяц (упрощённый производственный календарь).
 *
 * Правила:
 *  - рабочая неделя: понедельник-пятница, 8 часов в день (40-часовая неделя);
 *  - нерабочие праздничные дни (ст. 112 ТК РФ) исключаются из нормы;
 *  - предпраздничный рабочий день короче на 1 час.
 */
class ProductionCalendar(
    private val holidays: Set<LocalDate> = HolidaysRu.holidays,
    private val preholidays: Set<LocalDate> = HolidaysRu.preholidays,
) {

    fun isHoliday(date: LocalDate): Boolean = date in holidays

    fun isWorkingDay(date: LocalDate): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY && date !in holidays

    fun monthCalendarDays(year: Int, month: Int): List<LocalDate> {
        val yearMonth = YearMonth.of(year, month)
        return (1..yearMonth.lengthOfMonth()).map { day -> LocalDate.of(year, month, day) }
    }

    fun monthNorm(year: Int, month: Int): MonthNorm {
        val calendarDays = monthCalendarDays(year, month)
        val workingDays = calendarDays.filter(::isWorkingDay)
        val preholidayWorkdays = workingDays.count { it in preholidays }
        val normHours = (workingDays.size * HOURS_PER_WORKDAY - preholidayWorkdays).toDouble()
        return MonthNorm(year, month, calendarDays, workingDays, normHours)
    }

    /**
     * Календарные дни диапазона [range] без нерабочих праздничных дней.
     *
     * Используется для отпускных: нерабочие праздничные дни, попадающие на
     * период отпуска, не оплачиваются и не входят в число дней отпуска
     * (ст. 120 ТК РФ).
     */
    fun countCalendarDaysExcludingHolidays(range: ClosedRange<LocalDate>): Int {
        var count = 0
        var current = range.start
        while (!current.isAfter(range.endInclusive)) {
            if (current !in holidays) count++
            current = current.plusDays(1)
        }
        return count
    }
}
