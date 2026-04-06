package com.nobadhabbits.cornytask.features.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.data.cycle.CycleEntry
import com.nobadhabbits.cornytask.data.cycle.CycleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
class CycleViewModel(
    private val repository: CycleRepository
) : ViewModel() {

    private val predictor = CyclePredictor()

    val entries = repository.entries
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val predictions =
        entries
            .map { predictor.predict(it) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyMap()
            )

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    fun select(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggle(date: LocalDate) {
        viewModelScope.launch {
            repository.togglePeriod(date)
        }
    }
}