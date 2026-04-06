package com.nobadhabbits.cornytask.data.cycle

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CycleDao {

    @Query("SELECT * FROM cycle_entries ORDER BY date ASC")
    fun observeAll(): Flow<List<CycleEntry>>
    @Query("SELECT * FROM cycle_entries")
    suspend fun getAll(): List<CycleEntry>

    @Query("SELECT * FROM cycle_entries WHERE date = :date")
    suspend fun getByDate(date: LocalDate): List<CycleEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CycleEntry)

    @Delete
    suspend fun delete(entry: CycleEntry)
}