package com.payroll.calculator.ui.employees

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payroll.calculator.data.EmployeeRepository
import com.payroll.calculator.domain.Employee
import com.payroll.calculator.domain.PremiumType
import com.payroll.calculator.domain.VacationPeriod
import com.payroll.calculator.util.parseMoneyInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

data class EmployeeFormState(
    val id: String = EmployeeRepository.newId(),
    val name: String = "",
    val oklad: String = "",
    val rate: String = "1",
    val premiumType: PremiumType = PremiumType.FIXED,
    val premiumValue: String = "0",
    val avgDailyEarning: String = "",
    val vacations: List<VacationPeriod> = emptyList(),
    val loaded: Boolean = false,
    val nameError: Boolean = false,
    val okladError: Boolean = false,
) {
    companion object {
        fun fromDomain(employee: Employee): EmployeeFormState = EmployeeFormState(
            id = employee.id,
            name = employee.name,
            oklad = employee.oklad.toPlainString(),
            rate = employee.rate.toPlainString(),
            premiumType = employee.premiumType,
            premiumValue = employee.premiumValue.toPlainString(),
            avgDailyEarning = employee.avgDailyEarning?.toPlainString().orEmpty(),
            vacations = employee.vacations,
            loaded = true,
        )
    }
}

class EmployeeEditViewModel(private val repository: EmployeeRepository) : ViewModel() {

    var state by mutableStateOf(EmployeeFormState())
        private set

    private var isNew = true
    private var loadStarted = false

    fun load(employeeId: String?) {
        if (loadStarted) return
        loadStarted = true
        if (employeeId == null) {
            isNew = true
            state = EmployeeFormState(loaded = true)
            return
        }
        isNew = false
        viewModelScope.launch {
            repository.observeEmployee(employeeId).first()?.let { employee ->
                state = EmployeeFormState.fromDomain(employee)
            }
        }
    }

    fun onNameChange(value: String) {
        state = state.copy(name = value, nameError = false)
    }

    fun onOkladChange(value: String) {
        state = state.copy(oklad = value, okladError = false)
    }

    fun onRateChange(value: String) {
        state = state.copy(rate = value)
    }

    fun onPremiumTypeChange(value: PremiumType) {
        state = state.copy(premiumType = value)
    }

    fun onPremiumValueChange(value: String) {
        state = state.copy(premiumValue = value)
    }

    fun onAvgDailyEarningChange(value: String) {
        state = state.copy(avgDailyEarning = value)
    }

    fun addVacation(start: LocalDate, end: LocalDate) {
        val period = VacationPeriod(start, end)
        state = state.copy(vacations = (state.vacations + period).sortedBy { it.start })
    }

    fun removeVacation(period: VacationPeriod) {
        state = state.copy(vacations = state.vacations - period)
    }

    /** Возвращает true и сохраняет, либо false и выставляет ошибки валидации. */
    fun save(onSaved: () -> Unit) {
        val oklad = parseMoneyInput(state.oklad)
        val nameError = state.name.isBlank()
        val okladError = oklad == null || oklad < BigDecimal.ZERO
        if (nameError || okladError) {
            state = state.copy(nameError = nameError, okladError = okladError)
            return
        }

        val rate = parseMoneyInput(state.rate)?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE
        val premiumValue = parseMoneyInput(state.premiumValue) ?: BigDecimal.ZERO
        val avgDaily = parseMoneyInput(state.avgDailyEarning)

        val employee = Employee(
            id = state.id,
            name = state.name.trim(),
            oklad = oklad!!,
            rate = rate,
            vacations = state.vacations,
            premiumType = state.premiumType,
            premiumValue = premiumValue,
            avgDailyEarning = avgDaily,
        )

        viewModelScope.launch {
            repository.save(employee, isNew)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(state.id)
            onDeleted()
        }
    }
}
