package com.payroll.calculator.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.payroll.calculator.PayrollApp
import com.payroll.calculator.data.EmployeeRepository

/** Создаёт ViewModel, передавая ей [EmployeeRepository] из [PayrollApp]. */
@Composable
inline fun <reified VM : ViewModel> payrollViewModel(
    crossinline create: (EmployeeRepository) -> VM,
): VM {
    val repository = (LocalContext.current.applicationContext as PayrollApp).repository
    val factory = remember(repository) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return create(repository) as T
            }
        }
    }
    return viewModel(factory = factory)
}
