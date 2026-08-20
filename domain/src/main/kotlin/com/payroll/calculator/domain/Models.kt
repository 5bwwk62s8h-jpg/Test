package com.payroll.calculator.domain

import java.math.BigDecimal
import java.time.LocalDate

/** Способ расчёта премии сотрудника. */
enum class PremiumType {
    /** Фиксированная сумма. */
    FIXED,

    /** Процент от полного оклада (пропорционально ставке). */
    PERCENT_OKLAD,

    /** Процент от начисления за фактически отработанное время. */
    PERCENT_WORKED,
}

/** Период отпуска сотрудника (включительно). */
data class VacationPeriod(
    val start: LocalDate,
    val end: LocalDate,
) {
    init {
        require(!end.isBefore(start)) {
            "Дата окончания отпуска $end раньше даты начала $start"
        }
    }

    /** Пересечение периода отпуска с месяцем (year, month), либо null. */
    fun clipToMonth(year: Int, month: Int): ClosedRange<LocalDate>? {
        val monthStart = LocalDate.of(year, month, 1)
        val monthEnd = monthStart.plusMonths(1).minusDays(1)
        if (end.isBefore(monthStart) || start.isAfter(monthEnd)) return null
        val clippedStart = if (start.isAfter(monthStart)) start else monthStart
        val clippedEnd = if (end.isBefore(monthEnd)) end else monthEnd
        return clippedStart..clippedEnd
    }
}

/**
 * Сотрудник и параметры для расчёта затрат по ФОТ за месяц.
 *
 * @param oklad оклад за полную норму при ставке 1.0
 * @param rate ставка (0.5, 1.0, 1.5 ...)
 * @param avgDailyEarning средний дневной заработок для расчёта отпускных;
 *   если null — используется упрощённая оценка oklad*rate/29.3
 */
data class Employee(
    val id: String,
    val name: String,
    val oklad: BigDecimal,
    val rate: BigDecimal = BigDecimal.ONE,
    val vacations: List<VacationPeriod> = emptyList(),
    val premiumType: PremiumType = PremiumType.FIXED,
    val premiumValue: BigDecimal = BigDecimal.ZERO,
    val avgDailyEarning: BigDecimal? = null,
) {
    init {
        require(oklad >= BigDecimal.ZERO) { "Оклад не может быть отрицательным: $oklad" }
        require(rate > BigDecimal.ZERO) { "Ставка должна быть положительной: $rate" }
    }
}
