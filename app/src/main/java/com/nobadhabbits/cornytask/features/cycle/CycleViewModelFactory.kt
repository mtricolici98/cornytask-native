package com.nobadhabbits.cornytask.features.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nobadhabbits.cornytask.data.cycle.CycleRepository

class CycleViewModelFactory(
    private val repository: CycleRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CycleViewModel(repository) as T
    }
}