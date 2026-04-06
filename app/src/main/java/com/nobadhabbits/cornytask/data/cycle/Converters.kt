package com.nobadhabbits.cornytask.data.cycle

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toDate(value: String): LocalDate = LocalDate.parse(value)
}