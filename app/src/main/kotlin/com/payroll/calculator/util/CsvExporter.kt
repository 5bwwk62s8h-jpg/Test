package com.payroll.calculator.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.payroll.calculator.domain.EmployeeResult
import com.payroll.calculator.domain.PayrollReport
import java.io.File
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val CSV_MONEY_FORMAT = DecimalFormat(
    "0.00",
    DecimalFormatSymbols(Locale.ROOT).apply { decimalSeparator = '.' },
)

private fun BigDecimal.csv(): String = CSV_MONEY_FORMAT.format(this)

private val HEADERS = listOf(
    "Сотрудник", "Норма, ч", "Отработано, ч", "Дней отпуска",
    "Оклад за отраб. время", "Отпускные", "Премия", "Страх. взносы", "Итого",
)

private fun row(result: EmployeeResult): List<String> = listOf(
    result.employee.name,
    result.normHours.toString(),
    result.workedHours.toString(),
    result.vacationCalendarDays.toString(),
    result.baseSalary.csv(),
    result.vacationPay.csv(),
    result.premium.csv(),
    result.insuranceContributions.csv(),
    result.totalWithContributions.csv(),
)

private fun escape(field: String): String =
    if (field.contains(';') || field.contains('"') || field.contains('\n')) {
        "\"${field.replace("\"", "\"\"")}\""
    } else {
        field
    }

/** Формирует CSV-отчёт по ФОТ и делится им через системный диалог "Отправить". */
object CsvExporter {

    fun buildCsv(report: PayrollReport): String = buildString {
        append('﻿') // BOM для корректной кодировки в Excel
        appendLine(HEADERS.joinToString(";", transform = ::escape))
        report.results.forEach { result ->
            appendLine(row(result).joinToString(";", transform = ::escape))
        }
        appendLine()
        appendLine(listOf("Итого оклады", report.totalBaseSalary.csv()).joinToString(";"))
        appendLine(listOf("Итого отпускные", report.totalVacationPay.csv()).joinToString(";"))
        appendLine(listOf("Итого премии", report.totalPremium.csv()).joinToString(";"))
        if (report.insuranceRate.signum() != 0) {
            appendLine(listOf("Итого страх. взносы", report.totalInsuranceContributions.csv()).joinToString(";"))
            appendLine(listOf("ИТОГО ФОТ (со взносами)", report.totalFotWithContributions.csv()).joinToString(";"))
        } else {
            appendLine(listOf("ИТОГО ФОТ", report.totalFot.csv()).joinToString(";"))
        }
    }

    fun share(context: Context, report: PayrollReport) {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "fot_${report.year}_${"%02d".format(report.month)}.csv")
        file.writeText(buildCsv(report))

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Отправить отчёт по ФОТ"))
    }
}
