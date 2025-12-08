package com.example.cornytask_v2.features.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.cornytask_v2.R
import com.example.cornytask_v2.ui.theme.Purple40

class TodoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            TodoWidgetContent()
        }
    }

    @Composable
    private fun TodoWidgetContent() {
        val prefs = currentState<Preferences>()
        val userCoins = prefs[TodoWidgetStateKeys.userCoinsKey] ?: "0"
        val todoCount = prefs[TodoWidgetStateKeys.todoCountKey] ?: 0

        val textColor = ColorProvider(Color(Purple40.value))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .padding(16.dp)
        ) {

            // Header ---------------------------------------
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My TODOs",
                    style = TextStyle(color = textColor),
                    modifier = GlanceModifier.defaultWeight()
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
                        .clickable(actionRunCallback<AddTodoAction>())
                )
            }

            Spacer(GlanceModifier.height(10.dp))

            // Body -----------------------------------------
            if (todoCount > 0) {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .cornerRadius(4.dp)
                        .padding(top = 8.dp)
                ) {
                    itemsIndexed((0 until todoCount).toList()) { index, _ ->
                        val id = prefs[TodoWidgetStateKeys.todoIdKey(index)] ?: ""
                        val title = prefs[TodoWidgetStateKeys.todoTitleKey(index)] ?: ""
                        val isCompleted = prefs[TodoWidgetStateKeys.todoCompletedKey(index)] ?: false
                        val reward = prefs[TodoWidgetStateKeys.todoRewardKey(index)] ?: 0

                        TodoWidgetItem(id, title, isCompleted, reward, textColor)
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
        id: String,
        title: String,
        isCompleted: Boolean,
        reward: Int,
        textColor: ColorProvider
    ) {
        Column() {

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(8.dp)
                .padding(vertical = 2.dp)
                .background(ColorProvider(Color(0x80FFFFFF), Color(0x80FFFFFF))) // translucent card look
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckBox(
                checked = isCompleted,
                onCheckedChange = actionRunCallback<CompleteTodoAction>(
                    parameters = actionParametersOf(
                        todoIdKey to id,
                        isCompletedKey to !isCompleted
                    )
                )
            )

            Spacer(GlanceModifier.width(8.dp))

            Text(
                title,
                style = TextStyle(color = textColor),
                modifier = GlanceModifier.defaultWeight()
            )

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
}

val todoIdKey = ActionParameters.Key<String>("todoId")
val isCompletedKey = ActionParameters.Key<Boolean>("isCompleted")
