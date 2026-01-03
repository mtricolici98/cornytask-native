package com.nobadhabbits.cornytask.features.todo

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nobadhabbits.cornytask.R
import com.nobadhabbits.cornytask.data.Reward
import com.nobadhabbits.cornytask.data.Todo
import com.nobadhabbits.cornytask.features.rewards.RewardViewModel
import com.nobadhabbits.cornytask.ui.theme.Purple40
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val positiveEmojis = listOf("🎉", "✨", "🚀")
private val positiveMessages = listOf("Great job!", "You rock!", "Awesome!", "Keep it up!", "Fantastic!")
private val sadEmojis = listOf("😢", "🙏", "💪", "🤔")
private val negativeMessages = listOf(
    "You'll surely do it soon!",
    "No worries, you'll get it next time!",
    "It's okay, keep trying!",
    "One step back, two steps forward!",
    "Don't give up!"
)

@Composable
fun TodoScreen(
    todoViewModel: TodoViewModel = viewModel(factory = TodoViewModelFactory(LocalContext.current)),
    rewardViewModel: RewardViewModel = viewModel(),
) {
    val todos by todoViewModel.todos.collectAsState()
    val user by todoViewModel.user.collectAsState()
    val rewards by rewardViewModel.rewards.collectAsState()
    var showDialog by remember { mutableStateOf<Todo?>(null) }
    val context = LocalContext.current


    val currentCoins = user?.coins ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        if (todos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You have not created any TODOs yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(todos, key = { todo -> todo.id }) { todo ->
                    TodoItem(todo = todo, onLongPress = { showDialog = todo }) { isChecked ->
                        todoViewModel.onTodoCompleted(todo, isChecked)
                        makeToast(context, isChecked, todo, rewards, currentCoins)

                    }
                }
            }
        }
    }

    showDialog?.let { todo ->
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text("What do you want to do?") },
            text = { Text("You can choose to edit or delete the TODO.") },
            confirmButton = {
                TextButton(onClick = { todoViewModel.onDeleteTodo(todo); showDialog = null }) {
                    Text("Delete", color = Purple40)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val intent = Intent(context, EditTodoActivity::class.java)
                    intent.putExtra("todoId", todo.id)
                    context.startActivity(intent)
                    showDialog = null
                }) {
                    Text("Edit", color = Purple40)
                }
            }
        )
    }
}

private fun LazyItemScope.makeToast(
    context: Context,
    checked: Boolean,
    todo: Todo,
    rewards: List<Reward>,
    currentCoins: Int
) {
    if (checked) {
        // TODO: Handle the reward reached text as a notification instead
        val anyRewardReached = rewards.count { it -> it.isFavorite && it.cost <= currentCoins }
        val randomEmoji = positiveEmojis.random()
        val randomMessage = positiveMessages.random()
        var rewardReachedText = ""
        if (anyRewardReached > 1) {
            rewardReachedText = "\nYou've earned enough for ${anyRewardReached} favourite rewards!"
        } else if (anyRewardReached == 1) {
            rewardReachedText = "\nYou've earned enough for one favourite reward!"
        }
        val toastMessage = "$randomEmoji $randomMessage You've earned ${todo.rewardCoins} coins! $randomEmoji$rewardReachedText"
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    } else {
        val randomEmoji = sadEmojis.random()
        val randomMessage = negativeMessages.random()
        val toastMessage = "$randomEmoji $randomMessage"
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

}

private fun formatDueDateText(dueDate: Date): String {
    val now = System.currentTimeMillis()
    val due = dueDate.time
    val diff = due - now

    if (diff < 0) return "Overdue"

    val minutes = diff / 1000 / 60
    val hours = minutes / 60

    val dueCalendar = Calendar.getInstance().apply { time = dueDate }
    val nowCalendar = Calendar.getInstance()

    val isToday = dueCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
            dueCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR)

    val isTomorrow = dueCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) &&
            dueCalendar.get(Calendar.DAY_OF_YEAR) == nowCalendar.get(Calendar.DAY_OF_YEAR) + 1

    return when {
        minutes < 60 -> "Due in $minutes minutes"
        hours < 24 && isToday -> "Due in $hours hours"
        isTomorrow -> "Tomorrow at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(dueDate)}"
        else -> "Due: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dueDate)}"
    }
}

@Composable
private fun TodoItem(todo: Todo, onLongPress: () -> Unit, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(todo.title) },
        supportingContent = {
            Column {
                Text(todo.description)
                todo.dueDate?.let {
                    Text(formatDueDateText(it))
                }
            }
        },
        leadingContent = {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = onCheckedChange
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(todo.rewardCoins.toString())
                Spacer(Modifier.width(4.dp))
                Image(
                    painter = painterResource(id = R.drawable.unicorn_small),
                    contentDescription = "Reward coins",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { onLongPress() })
        }
    )
}
