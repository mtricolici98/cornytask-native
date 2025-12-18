package com.example.cornytask_v2.features.todo

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EditTodoViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditTodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditTodoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
