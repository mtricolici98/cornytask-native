package com.example.cornytask_v2.features.actions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.cornytask_v2.features.todo.TodoRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AddTaskReceiver : BroadcastReceiver() {

    private val todoRepository = TodoRepository()

    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("task_title")
        val rewardAmount = intent.getIntExtra("reward_amount", 0)

        if (taskTitle != null) {
            GlobalScope.launch {
                todoRepository.addTodo(taskTitle, "", rewardAmount)
            }
        }
    }
}
