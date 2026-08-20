package com.payroll.calculator

import android.app.Application
import com.payroll.calculator.data.AppDatabase
import com.payroll.calculator.data.EmployeeRepository

class PayrollApp : Application() {

    lateinit var repository: EmployeeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        repository = EmployeeRepository(database.employeeDao())
    }
}
