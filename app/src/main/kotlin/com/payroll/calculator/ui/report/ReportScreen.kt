package com.payroll.calculator.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.payroll.calculator.R
import com.payroll.calculator.domain.EmployeeResult
import com.payroll.calculator.domain.PayrollReport
import com.payroll.calculator.ui.payrollViewModel
import com.payroll.calculator.ui.theme.PayrollTheme
import com.payroll.calculator.util.CsvExporter
import com.payroll.calculator.util.formatHours
import com.payroll.calculator.util.formatMoney
import com.payroll.calculator.util.monthNameRu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen() {
    val viewModel = payrollViewModel(::ReportViewModel)
    val params by viewModel.reportParams.collectAsState()
    val report by viewModel.report.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.report_title)) }) },
        floatingActionButton = {
            if (report != null) {
                ExtendedFloatingActionButton(
                    onClick = { CsvExporter.share(context, report!!) },
                    icon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                    text = { Text(stringResource(R.string.report_export_csv)) },
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            MonthSelector(
                year = params.year,
                month = params.month,
                onPrev = {
                    val prev = java.time.LocalDate.of(params.year, params.month, 1).minusMonths(1)
                    viewModel.setMonth(prev.year, prev.monthValue)
                },
                onNext = {
                    val next = java.time.LocalDate.of(params.year, params.month, 1).plusMonths(1)
                    viewModel.setMonth(next.year, next.monthValue)
                },
            )

            OutlinedTextField(
                value = params.insuranceRate,
                onValueChange = viewModel::setInsuranceRate,
                label = { Text(stringResource(R.string.report_insurance_rate)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.size(8.dp))

            val currentReport = report
            if (currentReport == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.report_empty),
                        modifier = Modifier.padding(32.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { SummaryCards(currentReport) }
                    items(currentReport.results, key = { it.employee.id }) { result ->
                        EmployeeResultCard(result)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) { Icon(Icons.Filled.ChevronLeft, contentDescription = null) }
        Text(
            "${monthNameRu(month).replaceFirstChar(Char::uppercase)} $year",
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, contentDescription = null) }
    }
}

@Composable
private fun SummaryCards(report: PayrollReport) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryTile(stringResource(R.string.report_summary_base), report.totalBaseSalary.formatMoney(), Modifier.weight(1f))
            SummaryTile(stringResource(R.string.report_summary_vacation), report.totalVacationPay.formatMoney(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryTile(stringResource(R.string.report_summary_premium), report.totalPremium.formatMoney(), Modifier.weight(1f))
            SummaryTile(
                stringResource(R.string.report_summary_total),
                report.totalFotWithContributions.formatMoney(),
                Modifier.weight(1f),
                emphasize = true,
            )
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasize) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
                color = if (emphasize) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmployeeResultCard(result: EmployeeResult) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(result.employee.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    result.totalWithContributions.formatMoney(),
                    style = MaterialTheme.typography.titleMedium,
                    color = PayrollTheme.extendedColors.accentStrong,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (result.vacationCalendarDays > 0) {
                Spacer(Modifier.size(6.dp))
                Box(
                    modifier = Modifier
                        .background(PayrollTheme.extendedColors.flagSoft, RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "отпуск ${result.vacationCalendarDays} дн. · отработано ${result.workedHours.formatHours()} из ${result.normHours.formatHours()} ч",
                        style = MaterialTheme.typography.labelSmall,
                        color = PayrollTheme.extendedColors.flag,
                    )
                }
            }

            Spacer(Modifier.size(10.dp))
            ResultLine(stringResource(R.string.report_base_salary), result.baseSalary.formatMoney())
            ResultLine(stringResource(R.string.report_vacation_pay), result.vacationPay.formatMoney())
            ResultLine(stringResource(R.string.report_premium), result.premium.formatMoney())
            if (result.insuranceContributions.signum() != 0) {
                ResultLine(stringResource(R.string.report_insurance), result.insuranceContributions.formatMoney())
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
