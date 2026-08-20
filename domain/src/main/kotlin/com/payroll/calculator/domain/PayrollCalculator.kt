package com.payroll.calculator.domain

import java.math.BigDecimal
import java.math.RoundingMode

/** ст. 139 ТК РФ / методика Росстата — среднемесячное число календарных дней. */
private val AVG_MONTH_CALENDAR_DAYS = BigDecimal("29.3")
private val HUNDRED = BigDecimal(100)

private fun BigDecimal.money(): BigDecimal = setScale(2, RoundingMode.HALF_UP)

/** Разбивка ожидаемых затрат по одному сотруднику за месяц. */
data class EmployeeResult(
    val employee: Employee,
    val normHours: Double,
    val workedHours: Double,
    val vacationCalendarDays: Int,
    val baseSalary: BigDecimal,
    val vacationPay: BigDecimal,
    val premium: BigDecimal,
    val insuranceContributions: BigDecimal = BigDecimal.ZERO,
) {
    val total: BigDecimal get() = baseSalary + vacationPay + premium
    val totalWithContributions: BigDecimal get() = total + insuranceContributions
}

/** Итоговый отчёт по ФОТ за месяц. */
data class PayrollReport(
    val year: Int,
    val month: Int,
    val results: List<EmployeeResult>,
    val insuranceRate: BigDecimal = BigDecimal.ZERO,
) {
    val totalBaseSalary: BigDecimal get() = results.sumMoney { it.baseSalary }
    val totalVacationPay: BigDecimal get() = results.sumMoney { it.vacationPay }
    val totalPremium: BigDecimal get() = results.sumMoney { it.premium }
    val totalInsuranceContributions: BigDecimal get() = results.sumMoney { it.insuranceContributions }
    val totalFot: BigDecimal get() = results.sumMoney { it.total }
    val totalFotWithContributions: BigDecimal get() = results.sumMoney { it.totalWithContributions }

    private inline fun List<EmployeeResult>.sumMoney(selector: (EmployeeResult) -> BigDecimal): BigDecimal =
        fold(BigDecimal.ZERO) { acc, result -> acc + selector(result) }
}

/**
 * Считает ожидаемые затраты по ФОТ за месяц с учётом отпусков, нормы часов
 * и премий.
 */
class PayrollCalculator(
    private val calendar: ProductionCalendar = ProductionCalendar(),
) {

    fun calculateEmployee(employee: Employee, year: Int, month: Int): EmployeeResult {
        val monthNorm = calendar.monthNorm(year, month)
        val normHours = monthNorm.normHours

        var vacationCalendarDays = 0
        var vacationMissedHours = 0.0
        for (vacation in employee.vacations) {
            val clipped = vacation.clipToMonth(year, month) ?: continue
            vacationCalendarDays += calendar.countCalendarDaysExcludingHolidays(clipped)
            vacationMissedHours += monthNorm.workingDays.count { it in clipped } * 8
        }

        val workedHours = (normHours - vacationMissedHours).coerceAtLeast(0.0)

        val baseSalary = if (normHours > 0) {
            (employee.oklad * employee.rate * BigDecimal(workedHours) / BigDecimal(normHours)).money()
        } else {
            BigDecimal.ZERO
        }

        val avgDaily = employee.avgDailyEarning ?: (employee.oklad * employee.rate / AVG_MONTH_CALENDAR_DAYS).money()
        val vacationPay = (avgDaily * BigDecimal(vacationCalendarDays)).money()

        val premium = calculatePremium(employee, baseSalary)

        return EmployeeResult(
            employee = employee,
            normHours = normHours,
            workedHours = workedHours,
            vacationCalendarDays = vacationCalendarDays,
            baseSalary = baseSalary,
            vacationPay = vacationPay,
            premium = premium,
        )
    }

    private fun calculatePremium(employee: Employee, baseSalary: BigDecimal): BigDecimal = when (employee.premiumType) {
        PremiumType.FIXED -> employee.premiumValue.money()
        PremiumType.PERCENT_OKLAD -> (employee.oklad * employee.rate * employee.premiumValue / HUNDRED).money()
        PremiumType.PERCENT_WORKED -> (baseSalary * employee.premiumValue / HUNDRED).money()
    }

    fun calculateMonth(
        employees: List<Employee>,
        year: Int,
        month: Int,
        insuranceRate: BigDecimal = BigDecimal.ZERO,
    ): PayrollReport {
        val results = employees.map { employee ->
            val result = calculateEmployee(employee, year, month)
            if (insuranceRate.signum() != 0) {
                result.copy(insuranceContributions = (result.total * insuranceRate / HUNDRED).money())
            } else {
                result
            }
        }
        return PayrollReport(year = year, month = month, results = results, insuranceRate = insuranceRate)
    }
}
