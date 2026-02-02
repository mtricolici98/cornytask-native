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
import kotlinx.coroutines.launch
import java.util.Date

class EditTodoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TodoRepository(application)

    private var todoId: String? = null
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var rewardCoins by mutableStateOf("")


    var titleError by mutableStateOf<String?>(null)
    var rewardCoinsError by mutableStateOf<String?>(null)

    var dueDate by mutableStateOf<Date?>(null)

    fun loadTodo(todoId: String) {
        this.todoId = todoId
        viewModelScope.launch {
            val todo = repository.getTodo(todoId)
            todo?.let {
                title = it.title
                description = it.description
                rewardCoins = it.rewardCoins.toString()
                dueDate = it.dueDate
            }
        }
    }

    fun onDueDateChanged(date: Date) {
        dueDate = date
    }

    fun onTitleChanged(newTitle: String) {
        title = newTitle
        if (newTitle.length > 50) {
            titleError = "Title cannot be longer than 50 characters"
        } else {
            titleError = null
        }
    }

    fun onUpdateTodo(onSuccess: () -> Unit) {
        if (validate()) {
            viewModelScope.launch {
                val coins = rewardCoins.toInt()
                todoId?.let {
                    repository.updateTodo(it, title, description, coins, dueDate)
                    broadcastUpdate()
                    onSuccess()
                }
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

    private fun broadcastUpdate() {
        val intent = Intent(getApplication(), TodoWidgetReceiver::class.java).apply {
            action = "com.nobadhabbits.cornytask.ACTION_DATA_UPDATED"
        }
        getApplication<Application>().sendBroadcast(intent)
    }
}
