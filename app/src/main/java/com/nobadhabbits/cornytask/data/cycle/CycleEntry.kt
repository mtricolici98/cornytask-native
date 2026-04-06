package com.nobadhabbits.cornytask.data.cycle

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "cycle_entries")
data class CycleEntry(
    @PrimaryKey val date: LocalDate,
    val type: CycleType
)

enum class CycleType {
    PERIOD
}