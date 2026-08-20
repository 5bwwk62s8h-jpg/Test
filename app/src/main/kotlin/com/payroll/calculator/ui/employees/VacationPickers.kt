package com.payroll.calculator.ui.employees

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.payroll.calculator.R
import com.payroll.calculator.util.formatRu
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVacationDialog(onAdd: (LocalDate, LocalDate) -> Unit, onDismiss: () -> Unit) {
    var start by remember { mutableStateOf<LocalDate?>(null) }
    var end by remember { mutableStateOf<LocalDate?>(null) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.employee_vacations_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { pickingStart = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Text(" ${stringResource(R.string.common_from)}: ${start?.formatRu() ?: "—"}")
                }
                OutlinedButton(onClick = { pickingEnd = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Text(" ${stringResource(R.string.common_to)}: ${end?.formatRu() ?: "—"}")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = start != null && end != null && !end!!.isBefore(start),
                onClick = {
                    onAdd(start!!, end!!)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.employee_vacations_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )

    if (pickingStart) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (start ?: LocalDate.now()).toEpochMillis())
        DatePickerDialog(
            onDismissRequest = { pickingStart = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { start = it.toLocalDate() }
                    pickingStart = false
                }) { Text(stringResource(R.string.employee_save)) }
            },
            dismissButton = { TextButton(onClick = { pickingStart = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = state) }
    }

    if (pickingEnd) {
        val state = rememberDatePickerState(initialSelectedDateMillis = (end ?: start ?: LocalDate.now()).toEpochMillis())
        DatePickerDialog(
            onDismissRequest = { pickingEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { end = it.toLocalDate() }
                    pickingEnd = false
                }) { Text(stringResource(R.string.employee_save)) }
            },
            dismissButton = { TextButton(onClick = { pickingEnd = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) { DatePicker(state = state) }
    }
}
