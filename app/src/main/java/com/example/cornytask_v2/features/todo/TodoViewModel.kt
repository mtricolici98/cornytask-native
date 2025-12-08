package com.example.cornytask_v2.features.todo

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cornytask_v2.data.Todo
import com.example.cornytask_v2.data.User
import com.example.cornytask_v2.features.user.UserRepository
import com.example.cornytask_v2.features.widget.ACTION_DATA_UPDATED
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(private val context: Context) : ViewModel() {

    private val todoRepository = TodoRepository()
    private val userRepository = UserRepository()

    val todos: StateFlow<List<Todo>> = todoRepository.getTodosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val user: StateFlow<User?> = userRepository.getUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun onTodoCompleted(todo: Todo, isCompleted: Boolean) {
        viewModelScope.launch {
            todoRepository.updateTodoStatus(todo, isCompleted)

            if (isCompleted) {
                userRepository.addCoins(todo.rewardCoins)
            } else {
                userRepository.spendCoins(todo.rewardCoins)
            }

            // Notify the widget that data has changed
            sendDataUpdatedBroadcast()
        }
    }

    fun onDeleteTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.deleteTodo(todo)
            sendDataUpdatedBroadcast()
        }
    }

    fun onResetTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.resetTodo(todo)
            sendDataUpdatedBroadcast()
        }
    }

    private fun sendDataUpdatedBroadcast() {
        val intent = Intent(context, com.example.cornytask_v2.features.widget.TodoWidgetReceiver::class.java).apply {
            action = ACTION_DATA_UPDATED
        }
        context.sendBroadcast(intent)
    }
}