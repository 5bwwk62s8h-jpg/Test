package com.payroll.calculator.util

import java.math.BigDecimal

/** Разбирает денежное поле ввода ("120 000,50" / "120000.50") в BigDecimal. */
fun parseMoneyInput(raw: String): BigDecimal? {
    val normalized = raw.trim().replace(" ", "").replace(',', '.')
    if (normalized.isEmpty()) return null
    return normalized.toBigDecimalOrNull()
}

/** Пропускает во вводе только цифры, один разделитель дробной части и пробелы. */
fun sanitizeMoneyInput(raw: String): String {
    var seenSeparator = false
    return buildString {
        for (ch in raw) {
            when {
                ch.isDigit() -> append(ch)
                (ch == ',' || ch == '.') && !seenSeparator -> {
                    seenSeparator = true
                    append(ch)
                }
                else -> Unit
            }
        }
    }
}
