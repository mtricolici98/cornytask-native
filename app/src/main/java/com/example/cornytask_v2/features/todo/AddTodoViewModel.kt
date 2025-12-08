package com.example.cornytask_v2.features.todo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cornytask_v2.data.Todo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddTodoViewModel : ViewModel() {

    private val repository = TodoRepository()

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var rewardCoins by mutableStateOf("")

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
        _suggestions.value = emptyList()
    }

    fun onAddTodo(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val coins = rewardCoins.toIntOrNull() ?: 0
            if (title.isNotBlank() && coins > 0) {
                repository.addTodo(title, description, coins)
                onSuccess()
            }
        }
    }
}