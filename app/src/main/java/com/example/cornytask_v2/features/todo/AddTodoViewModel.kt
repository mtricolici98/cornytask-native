package com.example.cornytask_v2.features.todo

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cornytask_v2.data.Todo
import com.example.cornytask_v2.features.widget.TodoWidgetReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class AddTodoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TodoRepository()

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var rewardCoins by mutableStateOf("")
    var dueDate by mutableStateOf<Date?>(null)

    private val _suggestions = MutableStateFlow<List<Todo>>(emptyList())
    val suggestions: StateFlow<List<Todo>> = _suggestions

    private var searchJob: Job? = null

    fun onTitleChanged(newTitle: String) {
        title = newTitle
        searchJob?.cancel()
        if (newTitle.isNotBlank()) {
            searchJob = viewModelScope.launch {
                delay(300) // Debounce
                _suggestions.value = repository.getSuggestions(newTitle)
            }
        }
    }

    fun onSuggestionTapped(suggestion: Todo) {
        title = suggestion.title
        description = suggestion.description
        rewardCoins = suggestion.rewardCoins.toString()
        dueDate = suggestion.dueDate
        _suggestions.value = emptyList()
    }

    fun onAddTodo(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val coins = rewardCoins.toIntOrNull() ?: 0
            if (title.isNotBlank() && coins > 0) {
                repository.addTodo(title, description, coins, dueDate)
                broadcastUpdate()
                onSuccess()
            }
        }
    }

    fun onDueDateChanged(date: Date) {
        dueDate = date
    }

    private fun broadcastUpdate() {
        val intent = Intent(getApplication(), TodoWidgetReceiver::class.java).apply {
            action = "com.example.cornytask_v2.ACTION_DATA_UPDATED"
        }
        getApplication<Application>().sendBroadcast(intent)
    }
}
