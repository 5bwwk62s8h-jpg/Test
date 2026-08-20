package com.payroll.calculator.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RU_MONEY_SYMBOLS = DecimalFormatSymbols(Locale("ru", "RU")).apply {
    groupingSeparator = ' '
    decimalSeparator = ','
}
private val MONEY_FORMAT = DecimalFormat("#,##0.00", RU_MONEY_SYMBOLS)

/** Форматирует сумму как "120 000,00 ₽". */
fun BigDecimal.formatMoney(): String = "${MONEY_FORMAT.format(this)} ₽"

/** Форматирует часы без лишних нулей: 168 или 87.5. */
fun Double.formatHours(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

private val MONTH_NAMES_RU = listOf(
    "январь", "февраль", "март", "апрель", "май", "июнь",
    "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
)

fun monthNameRu(month: Int): String = MONTH_NAMES_RU[month - 1]

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru", "RU"))

fun LocalDate.formatRu(): String = format(DATE_FORMAT)
