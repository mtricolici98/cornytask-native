package com.nobadhabbits.cornytask.features.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CreateTodoFromAssistantActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.getStringExtra("action")
        val coins = intent.getStringExtra("coins")?.toIntOrNull()

        if (action != null && coins != null) {
            val todoRepository = TodoRepository()
            lifecycleScope.launch {
                todoRepository.addTodo(action, "", coins, null)
                finish()
            }
        } else {
            finish()
        }
    }
}