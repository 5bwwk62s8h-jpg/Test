package com.payroll.calculator.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payroll.calculator.data.EmployeeRepository
import com.payroll.calculator.domain.PayrollCalculator
import com.payroll.calculator.domain.PayrollReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.LocalDate

data class ReportParams(
    val year: Int,
    val month: Int,
    val insuranceRate: String,
)

class ReportViewModel(private val repository: EmployeeRepository) : ViewModel() {

    private val calculator = PayrollCalculator()
    private val today = LocalDate.now()

    private val params = MutableStateFlow(
        ReportParams(year = today.year, month = today.monthValue, insuranceRate = "0"),
    )
    val reportParams: StateFlow<ReportParams> = params

    val report: StateFlow<PayrollReport?> = combine(repository.observeEmployees(), params) { employees, p ->
        if (employees.isEmpty()) {
            null
        } else {
            val rate = p.insuranceRate.replace(',', '.').toBigDecimalOrNull() ?: BigDecimal.ZERO
            calculator.calculateMonth(employees, p.year, p.month, rate)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setMonth(year: Int, month: Int) {
        params.value = params.value.copy(year = year, month = month)
    }

    fun setInsuranceRate(rate: String) {
        params.value = params.value.copy(insuranceRate = rate)
    }
}
