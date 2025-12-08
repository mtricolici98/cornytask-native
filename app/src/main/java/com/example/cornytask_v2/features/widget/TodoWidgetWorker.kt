package com.example.cornytask_v2.features.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cornytask_v2.features.todo.TodoRepository
import com.example.cornytask_v2.features.user.UserRepository
import com.google.firebase.FirebaseApp

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
}