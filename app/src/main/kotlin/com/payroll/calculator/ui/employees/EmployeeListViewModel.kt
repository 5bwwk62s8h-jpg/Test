package com.payroll.calculator.ui.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payroll.calculator.data.EmployeeRepository
import com.payroll.calculator.domain.Employee
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmployeeListViewModel(private val repository: EmployeeRepository) : ViewModel() {

    val employees: StateFlow<List<Employee>> = repository.observeEmployees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteEmployee(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
