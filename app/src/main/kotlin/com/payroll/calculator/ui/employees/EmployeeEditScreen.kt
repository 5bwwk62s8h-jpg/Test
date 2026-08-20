package com.payroll.calculator.ui.employees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.payroll.calculator.R
import com.payroll.calculator.domain.PremiumType
import com.payroll.calculator.domain.VacationPeriod
import com.payroll.calculator.ui.components.ConfirmDialog
import com.payroll.calculator.ui.payrollViewModel
import com.payroll.calculator.ui.theme.PayrollTheme
import com.payroll.calculator.util.formatRu
import com.payroll.calculator.util.sanitizeMoneyInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeEditScreen(employeeId: String?, onDone: () -> Unit) {
    val viewModel = payrollViewModel(::EmployeeEditViewModel)
    LaunchedEffect(employeeId) { viewModel.load(employeeId) }
    val state = viewModel.state

    var showAddVacation by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (employeeId == null) R.string.employee_edit_title_new else R.string.employee_edit_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (employeeId != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.employee_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.employee_field_name)) },
                isError = state.nameError,
                supportingText = { if (state.nameError) Text(stringResource(R.string.employee_validation_name_required)) },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.oklad,
                onValueChange = { viewModel.onOkladChange(sanitizeMoneyInput(it)) },
                label = { Text(stringResource(R.string.employee_field_oklad)) },
                isError = state.okladError,
                supportingText = { if (state.okladError) Text(stringResource(R.string.employee_validation_oklad_required)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.rate,
                onValueChange = { viewModel.onRateChange(sanitizeMoneyInput(it)) },
                label = { Text(stringResource(R.string.employee_field_rate)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.avgDailyEarning,
                onValueChange = { viewModel.onAvgDailyEarningChange(sanitizeMoneyInput(it)) },
                label = { Text(stringResource(R.string.employee_field_avg_daily)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            PremiumTypeDropdown(value = state.premiumType, onChange = viewModel::onPremiumTypeChange)

            OutlinedTextField(
                value = state.premiumValue,
                onValueChange = { viewModel.onPremiumValueChange(sanitizeMoneyInput(it)) },
                label = { Text(stringResource(R.string.employee_field_premium_value)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            VacationsSection(
                vacations = state.vacations,
                onAdd = { showAddVacation = true },
                onRemove = viewModel::removeVacation,
            )

            Spacer(Modifier.padding(top = 4.dp))

            Button(onClick = { viewModel.save(onDone) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.employee_save))
            }
        }
    }

    if (showAddVacation) {
        AddVacationDialog(
            onAdd = { start, end -> viewModel.addVacation(start, end) },
            onDismiss = { showAddVacation = false },
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.employees_delete_confirm_title),
            body = stringResource(R.string.employees_delete_confirm_body),
            onConfirm = { viewModel.delete(onDone) },
            onDismiss = { showDeleteConfirm = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumTypeDropdown(value: PremiumType, onChange: (PremiumType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        PremiumType.FIXED to stringResource(R.string.employee_premium_fixed),
        PremiumType.PERCENT_OKLAD to stringResource(R.string.employee_premium_percent_oklad),
        PremiumType.PERCENT_WORKED to stringResource(R.string.employee_premium_percent_worked),
    )
    val label = options.first { it.first == value }.second

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.employee_field_premium_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (type, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onChange(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun VacationsSection(
    vacations: List<VacationPeriod>,
    onAdd: () -> Unit,
    onRemove: (VacationPeriod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.employee_vacations_title), style = MaterialTheme.typography.titleMedium)
        vacations.forEach { period ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PayrollTheme.extendedColors.surfaceSunken),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${period.start.formatRu()} — ${period.end.formatRu()}")
                    IconButton(onClick = { onRemove(period) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_delete))
                    }
                }
            }
        }
        TextButton(onClick = onAdd) {
            Text(stringResource(R.string.employee_vacations_add))
        }
    }
}
