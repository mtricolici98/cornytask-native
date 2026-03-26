package com.nobadhabbits.cornytask.features.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nobadhabbits.cornytask.features.todo.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = TodoRepository(context)
            val scheduler = NotificationScheduler(context)
            scheduler.cancelDailyMoodReminders()
            scheduler.scheduleDailyMoodReminders()
            scheduler.cancelWaterReminders()
            scheduler.scheduleWaterReminders()
            CoroutineScope(Dispatchers.IO).launch {
                val todos = repository.fetchAllTodos()
                todos.forEach {
                    scheduler.scheduleNotifications(it)
                }
            }
        }
    }
}