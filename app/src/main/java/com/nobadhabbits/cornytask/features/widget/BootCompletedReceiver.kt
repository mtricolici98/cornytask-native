package com.nobadhabbits.cornytask.features.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nobadhabbits.cornytask.features.todo.TodoRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            GlobalScope.launch {
                val repository = TodoRepository(context)
                val todos = repository.fetchAllTodos()
                val scheduler = NotificationScheduler(context)
                todos.forEach {
                    scheduler.scheduleNotifications(it)
                }
            }
        }
    }
}