package com.nobadhabbits.cornytask.data.cycle

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CycleEntry::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class CycleDatabase : RoomDatabase() {
    abstract fun dao(): CycleDao
}