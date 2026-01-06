package com.nobadhabbits.cornytask.features.todo

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.data.Todo
import com.nobadhabbits.cornytask.features.widget.TodoWidgetReceiver
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

    var titleError by mutableStateOf<String?>(null)
    var rewardCoinsError by mutableStateOf<String?>(null)

    private val _suggestions = MutableStateFlow<List<Todo>>(emptyList())
    val suggestions: StateFlow<List<Todo>> = _suggestions

    private var searchJob: Job? = null

    fun onTitleChanged(newTitle: String) {
        title = newTitle
        if (newTitle.length > 50) {
            titleError = "Title cannot be longer than 50 characters"
        } else {
            titleError = null
        }
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
    
    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    fun onAddTodo(onSuccess: () -> Unit) {
        if (validate()) {
            viewModelScope.launch {
                val coins = rewardCoins.toInt()
                repository.addTodo(title, description, coins, dueDate)
                broadcastUpdate()
                onSuccess()
            }
        }
    }

    private fun validate(): Boolean {
        var isValid = true
        if (title.isBlank()) {
            titleError = "Title is required"
            isValid = false
        } else {
            titleError = null
        }
        if (rewardCoins.isBlank()) {
            rewardCoinsError = "Reward is required"
            isValid = false
        } else if (rewardCoins.toIntOrNull() == null) {
            rewardCoinsError = "Reward must be a number"
            isValid = false
        } else {
            rewardCoinsError = null
        }
        return isValid
    }

    fun onDueDateChanged(date: Date) {
        dueDate = date
    }

    private fun broadcastUpdate() {
        val intent = Intent(getApplication(), TodoWidgetReceiver::class.java).apply {
            action = "com.nobadhabbits.cornytask.ACTION_DATA_UPDATED"
        }
        getApplication<Application>().sendBroadcast(intent)
    }
}
