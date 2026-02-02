package com.nobadhabbits.cornytask.features.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.data.Todo
import com.nobadhabbits.cornytask.features.todo.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val todoRepository = TodoRepository(application)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val todosByDate: StateFlow<Map<LocalDate, List<Todo>>> =
        todoRepository.getTodosFlow()
            .map { todos ->
                todos
                    .filter { it.dueDate != null }
                    .groupBy { todo ->
                        todo.dueDate!!
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyMap()
            )

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }
}