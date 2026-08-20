package com.payroll.calculator.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.payroll.calculator.domain.PremiumType
import java.math.BigDecimal
import java.time.LocalDate

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val oklad: BigDecimal,
    val rate: BigDecimal,
    val premiumType: PremiumType,
    val premiumValue: BigDecimal,
    val avgDailyEarning: BigDecimal?,
    val sortOrder: Long,
)

@Entity(
    tableName = "vacations",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class VacationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: String,
    val start: LocalDate,
    val end: LocalDate,
)

data class EmployeeWithVacations(
    @Embedded val employee: EmployeeEntity,
    @Relation(parentColumn = "id", entityColumn = "employeeId")
    val vacations: List<VacationEntity>,
)
