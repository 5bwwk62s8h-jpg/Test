package com.payroll.calculator.data

import androidx.room.TypeConverter
import com.payroll.calculator.domain.PremiumType
import java.math.BigDecimal
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun premiumTypeToString(value: PremiumType?): String? = value?.name

    @TypeConverter
    fun stringToPremiumType(value: String?): PremiumType? = value?.let(PremiumType::valueOf)
}
