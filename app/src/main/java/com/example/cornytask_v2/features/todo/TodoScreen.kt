package com.example.cornytask_v2.features.todo

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import com.example.cornytask_v2.R
import com.example.cornytask_v2.data.Reward
import com.example.cornytask_v2.data.Todo
import com.example.cornytask_v2.features.rewards.RewardViewModel
import com.example.cornytask_v2.ui.theme.Purple40

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
    navigateToRewards: () -> Unit = {}
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
            text = { Text("You can choose to reset the TODO, marking it as unfinished but keeping your coins or delete it.") },
            confirmButton = {
                TextButton(onClick = { todoViewModel.onDeleteTodo(todo); showDialog = null }) {
                    Text("Delete", color = Purple40)
                }
            },
            dismissButton = {
                TextButton(onClick = { todoViewModel.onResetTodo(todo); showDialog = null }) {
                    Text("Reset", color = Purple40)
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


@Composable
private fun TodoItem(todo: Todo, onLongPress: () -> Unit, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(todo.title) },
        supportingContent = { Text(todo.description) },
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
