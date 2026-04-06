package com.nobadhabbits.cornytask.data.cycle

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class CycleRepository(private val dao: CycleDao) {
    // ✅ reactive stream
    val entries: Flow<List<CycleEntry>> =
        dao.observeAll()

    suspend fun togglePeriod(date: LocalDate) {
        val existing = dao.getByDate(date)

        if (existing.isNotEmpty()) {
            existing.forEach { dao.delete(it) }
        } else {
            dao.insert(CycleEntry(date, CycleType.PERIOD))
        }
    }
}