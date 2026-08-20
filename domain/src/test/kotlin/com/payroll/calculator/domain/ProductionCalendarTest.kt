package com.payroll.calculator.domain

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductionCalendarTest {

    private val calendar = ProductionCalendar()

    @Test
    fun `august 2026 norm has no holidays`() {
        val norm = calendar.monthNorm(2026, 8)
        // Август 2026: 31 календарный день, начинается в субботу -> 21 рабочий день.
        assertEquals(31, norm.calendarDaysCount)
        assertEquals(21, norm.normDays)
        assertEquals(21 * 8.0, norm.normHours)
    }

    @Test
    fun `january 2026 norm excludes new year holidays`() {
        val norm = calendar.monthNorm(2026, 1)
        assertFalse(LocalDate.of(2026, 1, 1) in norm.workingDays)
        assertFalse(LocalDate.of(2026, 1, 8) in norm.workingDays)
        assertTrue(norm.normDays < 23)
    }

    @Test
    fun `preholiday shortens norm hours by one`() {
        val norm = calendar.monthNorm(2026, 2)
        assertTrue(LocalDate.of(2026, 2, 20) in norm.workingDays)
        assertEquals(norm.normDays * 8.0 - 1, norm.normHours)
    }

    @Test
    fun `is working day treats weekend as non-working`() {
        assertFalse(calendar.isWorkingDay(LocalDate.of(2026, 8, 15))) // суббота
        assertTrue(calendar.isWorkingDay(LocalDate.of(2026, 8, 17))) // понедельник
    }
}
