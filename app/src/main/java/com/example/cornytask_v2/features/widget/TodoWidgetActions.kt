package com.example.cornytask_v2.features.widget

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (isTablet(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val displayMetrics = context.resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels

                // Desired pop-up size
                val popupWidth = 600
                val popupHeight = 400

                // Position it on the right side
                val left = screenWidth - popupWidth - 50 // 50px margin from right edge
                val top = 100 // 100px from top
                val right = left + popupWidth
                val bottom = top + popupHeight

                val options = ActivityOptions.makeBasic()
                options.launchBounds = Rect(left, top, right, bottom)

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
