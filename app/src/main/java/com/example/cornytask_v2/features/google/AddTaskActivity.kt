package com.example.cornytask_v2.features.google

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.cornytask_v2.features.todo.TodoRepository
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class AddTaskActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Safely initialize Firebase
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        val taskTitle = intent.getStringExtra("taskTitle")
        val rewardAmountStr = intent.getStringExtra("rewardAmount")

        if (taskTitle != null && rewardAmountStr != null) {
            val rewardAmount = rewardAmountStr.toIntOrNull() ?: 0
            val todoRepository = TodoRepository()

            lifecycleScope.launch {
                todoRepository.addTodo(taskTitle, "", rewardAmount)
                // The task is added silently in the background.
                // The activity finishes immediately.
                finish()
            }
        } else {
            // If the required parameters are not provided, finish immediately.
            finish()
        }
    }
}
