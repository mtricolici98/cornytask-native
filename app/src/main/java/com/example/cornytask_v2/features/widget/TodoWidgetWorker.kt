package com.example.cornytask_v2.features.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cornytask_v2.features.todo.TodoRepository
import com.example.cornytask_v2.features.user.UserRepository
import com.google.firebase.FirebaseApp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodoWidgetWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Safely initialize Firebase only if it hasn'''t been initialized in this process yet.
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(TodoWidget::class.java)

        return try {
            val todoRepository = TodoRepository()
            val userRepository = UserRepository()

            val todos = todoRepository.fetchAllTodos()
            val user = userRepository.fetchCurrentUser()

            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs ->
                    // Do NOT clear. Instead overwrite keys safely.
                    prefs[TodoWidgetStateKeys.userCoinsKey] = user?.coins?.toString() ?: "0"
                    prefs[TodoWidgetStateKeys.todoCountKey] = todos.size

                    todos.forEachIndexed { index, todo ->
                        prefs[TodoWidgetStateKeys.todoIdKey(index)] = todo.id
                        prefs[TodoWidgetStateKeys.todoTitleKey(index)] = todo.title
                        prefs[TodoWidgetStateKeys.todoCompletedKey(index)] = todo.isCompleted
                        prefs[TodoWidgetStateKeys.todoDueDateKey(index)] = dateToString(todo.dueDate)
                        prefs[TodoWidgetStateKeys.todoRewardKey(index)] = todo.rewardCoins
                    }
                }
                TodoWidget().apply {
                    update(context, glanceId)
                }
            }

            Result.success()
        } catch (e: Exception) {
            // If the worker fails, it will be retried.
            Result.retry()
        }
    }


    private fun dateToString(date: Date?): String {
        if (date == null) {
            return "";
        }
        val now = System.currentTimeMillis()
        val due = date.time;
        val diff = due - now

        if (diff < 0) return "Overdue"

        val minutes = diff / 1000 / 60
        val hours = minutes / 60

        val dueCalendar = Calendar.getInstance().apply { time = date }
        val nowCalendar = Calendar.getInstance()

        val isToday = dueCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
                dueCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR)

        val isTomorrow = dueCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
                dueCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR) + 1

        return when {
            minutes < 60 -> "Due in $minutes minutes"
            hours < 24 && isToday -> "Due in $hours hours"
            isTomorrow -> "Tomorrow at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
            else -> "Due: ${SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)}"
        }
    }
}