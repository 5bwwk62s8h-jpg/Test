package com.payroll.calculator.domain

import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

private const val YEAR = 2026
private const val MONTH = 8 // 21 рабочий день, 168 часов нормы, без праздников

class PayrollCalculatorTest {

    private val calculator = PayrollCalculator()

    @Test
    fun `full month no vacation no premium`() {
        val employee = Employee(id = "1", name = "Тест", oklad = BigDecimal("100000"))
        val result = calculator.calculateEmployee(employee, YEAR, MONTH)
        assertEquals(BigDecimal("100000.00"), result.baseSalary)
        assertEquals(BigDecimal("0.00"), result.vacationPay)
        assertEquals(BigDecimal("0.00"), result.premium)
        assertEquals(BigDecimal("100000.00"), result.total)
    }

    @Test
    fun `part time rate scales salary`() {
        val employee = Employee(id = "2", name = "Тест 0.5", oklad = BigDecimal("100000"), rate = BigDecimal("0.5"))
        val result = calculator.calculateEmployee(employee, YEAR, MONTH)
        assertEquals(BigDecimal("50000.00"), result.baseSalary)
    }

    @Test
    fun `full month vacation zeroes base salary and pays vacation pay`() {
        val employee = Employee(
            id = "3",
            name = "В отпуске весь месяц",
            oklad = BigDecimal("150000"),
            vacations = listOf(VacationPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))),
            avgDailyEarning = BigDecimal("5000"),
        )
        val result = calculator.calculateEmployee(employee, YEAR, MONTH)
        assertEquals(BigDecimal("0.00"), result.baseSalary)
        assertEquals(31, result.vacationCalendarDays)
        assertEquals(BigDecimal("155000.00"), result.vacationPay)
    }

    @Test
    fun `partial vacation reduces base salary proportionally`() {
        val employee = Employee(
            id = "4",
            name = "Частичный отпуск",
            oklad = BigDecimal("210000"), // 210000 / 168ч = 1250 руб/час
            vacations = listOf(VacationPeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 23))),
            avgDailyEarning = BigDecimal("4000"),
        )
        val result = calculator.calculateEmployee(employee, YEAR, MONTH)
        val missedHours = result.normHours - result.workedHours
        assertEquals(10 * 8.0, missedHours)
        assertEquals(168 - 80.0, result.workedHours)
        val expectedBase = (BigDecimal("210000") * BigDecimal(88) / BigDecimal(168))
            .setScale(2, java.math.RoundingMode.HALF_UP)
        assertEquals(expectedBase, result.baseSalary)
        assertEquals(14, result.vacationCalendarDays) // 10..23 включительно
    }

    @Test
    fun `fixed premium is added on top of base salary`() {
        val employee = Employee(
            id = "5", name = "Премия фикс", oklad = BigDecimal("100000"),
            premiumType = PremiumType.FIXED, premiumValue = BigDecimal("15000"),
        )
        val result = calculator.calculateEmployee(employee, YEAR, MONTH)
        assertEquals(BigDecimal("15000.00"), result.premium)
        assertEquals(BigDecimal("115000.00"), result.total)
    }

    @Test
    fun `percent of oklad premium ignores vacation`() {
        val employee = Employee(
            id = "6", name = "Премия % от оклада", oklad = BigDecimal("100000"),
            premiumType = PremiumType.PERCENT_OKLAD, premiumValue = BigDecimal("20"),
        )
        val result = calculator.calculateEmployee(employee, YEAR, MONTH)
        assertEquals(BigDecimal("20000.00"), result.premium)
    }

    @Test
    fun `percent of worked premium scales with vacation`() {
        val employee = Employee(
            id = "7", name = "Премия % от отраб.", oklad = BigDecimal("168000"), // 1000 руб/час
            vacations = listOf(VacationPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))),
            premiumType = PremiumType.PERCENT_WORKED, premiumValue = BigDecimal("10"),
            avgDailyEarning = BigDecimal.ZERO,
        )
        val result = calculator.calculateEmployee(employee, YEAR, MONTH)
        assertEquals(BigDecimal("0.00"), result.baseSalary)
        assertEquals(BigDecimal("0.00"), result.premium)
    }

    @Test
    fun `insurance contributions applied on month total`() {
        val employees = listOf(
            Employee(id = "1", name = "A", oklad = BigDecimal("100000")),
            Employee(id = "2", name = "B", oklad = BigDecimal("50000")),
        )
        val report = calculator.calculateMonth(employees, YEAR, MONTH, insuranceRate = BigDecimal("30"))
        assertEquals(BigDecimal("150000.00"), report.totalFot)
        assertEquals(BigDecimal("45000.00"), report.totalInsuranceContributions)
        assertEquals(BigDecimal("195000.00"), report.totalFotWithContributions)
    }

    @Test
    fun `default avg daily earning used when not provided`() {
        val employee = Employee(
            id = "8",
            name = "Без указания среднего заработка",
            oklad = BigDecimal("29300"), // ровно 1000 руб/календарный день по формуле по умолчанию
            vacations = listOf(VacationPeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5))),
        )
        val result = calculator.calculateEmployee(employee, YEAR, MONTH)
        assertEquals(BigDecimal("5000.00"), result.vacationPay)
    }
}
