package com.payroll.calculator.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Transaction
    @Query("SELECT * FROM employees ORDER BY sortOrder ASC")
    fun observeEmployees(): Flow<List<EmployeeWithVacations>>

    @Transaction
    @Query("SELECT * FROM employees WHERE id = :id")
    fun observeEmployee(id: String): Flow<EmployeeWithVacations?>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM employees")
    suspend fun nextSortOrder(): Long

    @Query("SELECT sortOrder FROM employees WHERE id = :id")
    suspend fun sortOrderOf(id: String): Long?

    @Upsert
    suspend fun upsertEmployee(employee: EmployeeEntity)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteEmployee(id: String)

    @Query("DELETE FROM vacations WHERE employeeId = :employeeId")
    suspend fun deleteVacationsForEmployee(employeeId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVacations(vacations: List<VacationEntity>)

    @Delete
    suspend fun deleteVacation(vacation: VacationEntity)

    @Transaction
    suspend fun saveEmployeeWithVacations(employee: EmployeeEntity, vacations: List<VacationEntity>) {
        upsertEmployee(employee)
        deleteVacationsForEmployee(employee.id)
        if (vacations.isNotEmpty()) {
            insertVacations(vacations.map { it.copy(employeeId = employee.id) })
        }
    }
}
