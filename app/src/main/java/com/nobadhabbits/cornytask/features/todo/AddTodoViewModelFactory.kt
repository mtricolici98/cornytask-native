package com.nobadhabbits.cornytask.features.todo

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AddTodoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddTodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddTodoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
