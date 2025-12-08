package com.example.cornytask_v2.features.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import com.example.cornytask_v2.R
import com.example.cornytask_v2.data.Todo
import com.example.cornytask_v2.data.User
import com.example.cornytask_v2.features.todo.TodoRepository
import com.example.cornytask_v2.features.user.UserRepository
import com.google.firebase.FirebaseApp

class TodoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Safely initialize Firebase only if it hasn'''t been initialized in this process yet.
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }

        val todoRepository = TodoRepository()
        val userRepository = UserRepository()

        // Use the new one-shot fetch functions
        val todos = todoRepository.fetchAllTodos()
        val user = userRepository.fetchCurrentUser()

        provideContent {
            TodoWidgetContent(todos = todos, user = user)
        }
    }

    @Composable
    private fun TodoWidgetContent(todos: List<Todo>, user: User?) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("My TODOs", modifier = GlanceModifier.defaultWeight())
                user?.let {
                    Text("${it.coins}")
                    Image(provider = ImageProvider(R.drawable.unicorn_small), contentDescription = "Coins")
                }
            }

            Spacer(modifier = GlanceModifier.padding(8.dp))

            LazyColumn(modifier = GlanceModifier.defaultWeight()) {
                items(todos, itemId = { it.id.hashCode().toLong() }) { todo ->
                    TodoWidgetItem(todo = todo)
                }
            }

            Spacer(modifier = GlanceModifier.padding(8.dp))

            Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Image(
                    provider = ImageProvider(R.drawable.ic_launcher_foreground),
                    contentDescription = "Add a new TODO",
                    modifier = GlanceModifier.clickable(actionRunCallback<AddTodoAction>())                )
            }
        }
    }

    @Composable
    private fun TodoWidgetItem(todo: Todo) {
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            CheckBox(
                checked = todo.isCompleted,
                onCheckedChange = actionRunCallback<CompleteTodoAction>(
                    parameters = actionParametersOf(todoIdKey to todo.id, isCompletedKey to !todo.isCompleted)
                )
            )
            Text(todo.title, modifier = GlanceModifier.defaultWeight())
            Text("${todo.rewardCoins}")
            Image(provider = ImageProvider(R.drawable.unicorn_small), contentDescription = "Reward coins")
        }
    }
}

val todoIdKey = ActionParameters.Key<String>("todoId")
val isCompletedKey = ActionParameters.Key<Boolean>("isCompleted")
