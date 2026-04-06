package com.nobadhabbits.cornytask

import android.app.Application
import com.nobadhabbits.cornytask.di.AppContainer

class App : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}