package com.payroll.calculator.data

import com.payroll.calculator.domain.Employee
import com.payroll.calculator.domain.VacationPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class EmployeeRepository(private val dao: EmployeeDao) {

    fun observeEmployees(): Flow<List<Employee>> =
        dao.observeEmployees().map { list -> list.map { it.toDomain() } }

    fun observeEmployee(id: String): Flow<Employee?> =
        dao.observeEmployee(id).map { it?.toDomain() }

    suspend fun save(employee: Employee, isNew: Boolean) {
        val sortOrder = if (isNew) null else dao.sortOrderOf(employee.id)
        val entity = employee.toEntity(sortOrder ?: dao.nextSortOrder())
        val vacationEntities = employee.vacations.map { vacation ->
            VacationEntity(employeeId = employee.id, start = vacation.start, end = vacation.end)
        }
        dao.saveEmployeeWithVacations(entity, vacationEntities)
    }

    suspend fun delete(employeeId: String) = dao.deleteEmployee(employeeId)

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

private fun EmployeeWithVacations.toDomain(): Employee = Employee(
    id = employee.id,
    name = employee.name,
    oklad = employee.oklad,
    rate = employee.rate,
    vacations = vacations.map { VacationPeriod(it.start, it.end) },
    premiumType = employee.premiumType,
    premiumValue = employee.premiumValue,
    avgDailyEarning = employee.avgDailyEarning,
)

private fun Employee.toEntity(sortOrder: Long): EmployeeEntity = EmployeeEntity(
    id = id,
    name = name,
    oklad = oklad,
    rate = rate,
    premiumType = premiumType,
    premiumValue = premiumValue,
    avgDailyEarning = avgDailyEarning,
    sortOrder = sortOrder,
)
