package com.example.cornytask_v2.features.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.cornytask_v2.R
import com.example.cornytask_v2.ui.theme.Purple40
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.color.ColorProvider

class TodoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            TodoWidgetContent()
        }
    }
}

@Composable
private fun TodoWidgetContent() {
    val prefs = currentState<Preferences>()
    val userCoins = prefs[TodoWidgetStateKeys.userCoinsKey] ?: "0"
    val todoCount = prefs[TodoWidgetStateKeys.todoCountKey] ?: 0

    val todos = (0 until todoCount).map { index ->
        val id = prefs[TodoWidgetStateKeys.todoIdKey(index)] ?: ""
        val isCompleted = prefs[TodoWidgetStateKeys.todoCompletedKey(index)] ?: false
        TodoInfo(
            title = prefs[TodoWidgetStateKeys.todoTitleKey(index)] ?: "",
            isCompleted = isCompleted,
            dueDate = prefs[TodoWidgetStateKeys.todoDueDateKey(index)] ?: "",
            reward = prefs[TodoWidgetStateKeys.todoRewardKey(index)] ?: 0,
            onItemClicked = actionRunCallback<EditTodoAction>(
                parameters = actionParametersOf(
                    todoIdKey to id
                )
            ),
            onCheckedChange = actionRunCallback<CompleteTodoAction>(
                parameters = actionParametersOf(
                    todoIdKey to id,
                    isCompletedKey to !isCompleted
                )
            )
        )
    }

    TodoWidgetUi(
        userCoins = userCoins,
        todos = todos,
        onOpenApp = actionRunCallback<OpenAppAction>(),
        onAddTodo = actionRunCallback<AddTodoAction>()
    )
}

private data class TodoInfo(
    val title: String,
    val isCompleted: Boolean,
    val reward: Int,
    val dueDate: String,
    val onCheckedChange: Action?,
    val onItemClicked: Action?
)

@Composable
private fun TodoWidgetUi(
    userCoins: String,
    todos: List<TodoInfo>,
    onOpenApp: Action?,
    onAddTodo: Action?
) {
    val textColor = ColorProvider(Color(Purple40.value))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(16.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My TODOs",
                style = TextStyle(color = textColor),
                modifier = GlanceModifier.defaultWeight().let { if (onOpenApp != null) it.clickable(onOpenApp) else it }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(userCoins, style = TextStyle(color = textColor))
                Spacer(GlanceModifier.width(4.dp))
                Image(
                    provider = ImageProvider(R.drawable.unicorn_small),
                    contentDescription = "Coins",
                    modifier = GlanceModifier.size(22.dp)
                )
            }

            Image(
                provider = ImageProvider(R.drawable.material_add),
                contentDescription = "Add TODO",
                modifier = GlanceModifier
                    .size(24.dp)
                    .let { if (onAddTodo != null) it.clickable(onAddTodo) else it }
            )
        }

        Spacer(GlanceModifier.height(10.dp))

        if (todos.isNotEmpty()) {
            LazyColumn(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                items(todos) { todo ->
                    TodoWidgetItem(
                        title = todo.title,
                        isCompleted = todo.isCompleted,
                        reward = todo.reward,
                        dueDate = todo.dueDate,
                        textColor = textColor,
                        onCheckedChange = todo.onCheckedChange,
                        onEditTodo = todo.onItemClicked
                    )
                }
            }
        } else {
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No TODOs yet!", style = TextStyle(color = textColor))
            }
        }

        Spacer(GlanceModifier.height(10.dp))
    }
}

@Composable
private fun TodoWidgetItem(
    title: String,
    isCompleted: Boolean,
    reward: Int,
    dueDate: String,
    textColor: ColorProvider,
    onCheckedChange: Action?,
    onEditTodo: Action?
) {
    Column {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .background(ColorProvider(Color(0x80FFFFFF), Color(0x80FFFFFF))) // translucent card look
                .padding(8.dp).let { if (onEditTodo != null) it.clickable(onEditTodo) else it },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckBox(
                checked = isCompleted,
                onCheckedChange = onCheckedChange
            )

            Spacer(GlanceModifier.width(8.dp))

            Text(
                title,
                style = TextStyle(color = textColor),
                modifier = GlanceModifier.defaultWeight()
            )

            Text(
                dueDate,
                style = TextStyle(color = textColor),
            )

            Spacer(GlanceModifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$reward", style = TextStyle(color = textColor))
                Spacer(GlanceModifier.width(4.dp))
                Image(
                    provider = ImageProvider(R.drawable.unicorn_small),
                    contentDescription = "Reward coins",
                    modifier = GlanceModifier.size(22.dp)
                )
            }
        }
        Spacer(GlanceModifier.height(4.dp))
    }
}


val todoIdKey = ActionParameters.Key<String>("todoId")
val isCompletedKey = ActionParameters.Key<Boolean>("isCompleted")
