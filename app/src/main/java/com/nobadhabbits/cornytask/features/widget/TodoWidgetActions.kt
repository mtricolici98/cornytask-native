package com.nobadhabbits.cornytask.features.widget

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.nobadhabbits.cornytask.MainActivity
import com.nobadhabbits.cornytask.features.todo.AddTodoActivity
import com.nobadhabbits.cornytask.features.todo.EditTodoActivity
import com.nobadhabbits.cornytask.features.todo.TodoRepository
import com.nobadhabbits.cornytask.features.user.UserRepository
import com.google.firebase.FirebaseApp

const val ACTION_DATA_UPDATED = "com.nobadhabbits.cornytask.ACTION_DATA_UPDATED"

class CompleteTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Safely initialize Firebase only if it hasn't been initialized in this process yet.
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val todoId = parameters[todoIdKey] ?: return
        val isCompleted = parameters[isCompletedKey] ?: return

        val todoRepository = TodoRepository(context)
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

abstract class PopupActionCallback : ActionCallback {

    private fun isTablet(context: Context): Boolean {
        return context.resources.configuration.smallestScreenWidthDp >= 600
    }

    fun openActivity(context: Context,
                    activity: Class<out ComponentActivity>,
                 todoId: String?) {

        val intent = Intent(context, activity).apply {
            flags = Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("isFromWidget", true)
        }
        if (todoId != null) {
            intent.putExtra("todoId", todoId)
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
}

class AddTodoAction : PopupActionCallback() {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
       openActivity(context, AddTodoActivity::class.java, todoId = null)
    }

}


class EditTodoAction : PopupActionCallback() {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        openActivity(context, EditTodoActivity::class.java, parameters[todoIdKey])
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
