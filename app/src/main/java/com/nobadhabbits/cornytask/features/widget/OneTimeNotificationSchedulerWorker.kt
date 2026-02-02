package com.nobadhabbits.cornytask.features.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import com.nobadhabbits.cornytask.features.todo.TodoRepository
import androidx.core.content.edit

class OneTimeNotificationSchedulerWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val notificationsScheduled = prefs.getBoolean("one_time_notifications_scheduled", false)

            if (notificationsScheduled) {
                return Result.success()
            }
            
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            val todoRepository = TodoRepository(context)
            val notificationScheduler = NotificationScheduler(context)

            val todos = todoRepository.fetchAllTodos()
            todos.forEach { todo ->
                if (todo.dueDate != null) {
                    notificationScheduler.scheduleNotifications(todo)
                }
            }

            prefs.edit { putBoolean("one_time_notifications_scheduled", true) }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}