package com.example.cornytask_v2.features.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cornytask_v2.data.Todo
import com.example.cornytask_v2.features.user.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel : ViewModel() {

    private val todoRepository = TodoRepository()
    private val userRepository = UserRepository()

    val todos: StateFlow<List<Todo>> = todoRepository.getTodosFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onTodoCompleted(todo: Todo, isCompleted: Boolean) {
        viewModelScope.launch {
            // First, update the todo status in its own repository
            todoRepository.updateTodoStatus(todo, isCompleted)

            // Then, update the user'''s coins based on the action
            if (isCompleted) {
                userRepository.addCoins(todo.rewardCoins)
            } else {
                userRepository.spendCoins(todo.rewardCoins)
            }
        }
    }

    fun onDeleteTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.deleteTodo(todo)
        }
    }

    fun onResetTodo(todo: Todo) {
        viewModelScope.launch {
            todoRepository.resetTodo(todo)
        }
    }
}