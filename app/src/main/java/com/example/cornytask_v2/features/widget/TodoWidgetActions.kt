package com.example.cornytask_v2.features.widget

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import androidx.compose.ui.unit.IntOffset
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.cornytask_v2.MainActivity
import com.example.cornytask_v2.features.todo.AddTodoActivity
import com.example.cornytask_v2.features.todo.TodoRepository
import com.example.cornytask_v2.features.user.UserRepository
import com.google.firebase.FirebaseApp

const val ACTION_DATA_UPDATED = "com.example.cornytask_v2.ACTION_DATA_UPDATED"

class CompleteTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Safely initialize Firebase only if it hasn't been initialized in this process yet.
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val todoId = parameters[todoIdKey] ?: return
        val isCompleted = parameters[isCompletedKey] ?: return

        val todoRepository = TodoRepository()
        val userRepository = UserRepository()

        // Use the one-shot fetch function to get the specific todo
        val todo = todoRepository.fetchAllTodos().find { it.id == todoId } ?: return

        todoRepository.updateTodoStatus(todo, isCompleted)

        if (isCompleted) {
            userRepository.addCoins(todo.rewardCoins)
        } else {
            userRepository.spendCoins(todo.rewardCoins)
        }

        // Instead of updating the UI directly, trigger a background worker
        // to fetch the latest data and then update the widget.
        enqueueDataUpdateWorker(context)
    }
}

class AddTodoAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, AddTodoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("isFromWidget", true)
        }

        if (isTablet(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val displayMetrics = context.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels

                // Desired pop-up size
                val popupWidth = (screenWidth * 0.4).toInt()
                val popupHeight = (screenHeight * 0.5).toInt()
                val left = screenWidth - popupWidth - 50
                val top = 100

                val options = ActivityOptions.makeBasic()
                options.launchBounds = Rect(left, top, left + popupWidth, top + popupHeight)

                context.startActivity(intent, options.toBundle())
            } else {
                context.startActivity(intent)
            }
        } else {
            context.startActivity(intent)
        }
    }

    private fun isTablet(context: Context): Boolean {
        return context.resources.configuration.smallestScreenWidthDp >= 600
    }
}

class OpenAppAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
