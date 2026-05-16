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

            val notificationScheduler = NotificationScheduler(context)
            notificationScheduler.cancelDailyMoodReminders()
            notificationScheduler.scheduleDailyMoodReminders()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}