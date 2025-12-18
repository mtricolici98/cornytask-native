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
import kotlinx.coroutines.launch

class EditTodoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TodoRepository()

    private var todoId: String? = null
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var rewardCoins by mutableStateOf("")

    fun loadTodo(todoId: String) {
        this.todoId = todoId
        viewModelScope.launch {
            val todo = repository.getTodo(todoId)
            todo?.let {
                title = it.title
                description = it.description
                rewardCoins = it.rewardCoins.toString()
            }
        }
    }

    fun onTitleChanged(newTitle: String) {
        title = newTitle
    }

    fun onUpdateTodo(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val coins = rewardCoins.toIntOrNull() ?: 0
            if (title.isNotBlank() && coins > 0) {
                todoId?.let {
                    repository.updateTodo(it, title, description, coins)
                    broadcastUpdate()
                    onSuccess()
                }
            }
        }
    }

    private fun broadcastUpdate() {
        val intent = Intent(getApplication(), TodoWidgetReceiver::class.java).apply {
            action = "com.example.cornytask_v2.ACTION_DATA_UPDATED"
        }
        getApplication<Application>().sendBroadcast(intent)
    }
}
