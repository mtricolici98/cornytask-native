package com.example.cornytask_v2.features.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.cornytask_v2.features.todo.AddTodoActivity
import com.example.cornytask_v2.features.todo.TodoRepository
import com.example.cornytask_v2.features.user.UserRepository
import com.google.firebase.FirebaseApp

class CompleteTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Safely initialize Firebase only if it hasn'''t been initialized in this process yet.
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

        // Refresh the widget to show the latest data
        TodoWidget().update(context, glanceId)
    }
}

class AddTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, AddTodoActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // Safely initialize Firebase only if it hasn'''t been initialized in this process yet.
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        TodoWidget().update(context, glanceId)
    }
}