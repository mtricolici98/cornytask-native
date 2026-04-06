package com.nobadhabbits.cornytask.di

import android.content.Context
import androidx.room.Room
import com.nobadhabbits.cornytask.data.cycle.*

class AppContainer(context: Context) {

    private val db: CycleDatabase = Room.databaseBuilder(
        context,
        CycleDatabase::class.java,
        "cycle_db"
    )
        .fallbackToDestructiveMigration() // dev only
        .build()

    private val dao: CycleDao = db.dao()

    val cycleRepository = CycleRepository(dao)
}