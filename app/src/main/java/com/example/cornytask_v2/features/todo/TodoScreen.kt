package com.example.cornytask_v2.features.todo

import androidx.compose.foundation.Image
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

@Composable
fun TodoScreen(
    todoViewModel: TodoViewModel = viewModel(factory = TodoViewModelFactory(LocalContext.current)),
    rewardViewModel: RewardViewModel = viewModel()
) {
    val todos by todoViewModel.todos.collectAsState()
    val user by todoViewModel.user.collectAsState()
    val rewards by rewardViewModel.rewards.collectAsState()
    var showDialog by remember { mutableStateOf<Todo?>(null) }

    val favoriteRewards = rewards.filter { it.isFavorite }
    val currentCoins = user?.coins ?: 0

    Column(modifier = Modifier.fillMaxSize()) {
        if (favoriteRewards.isNotEmpty()) {
            FavoriteRewardsProgress(
                favoriteRewards = favoriteRewards,
                currentCoins = currentCoins
            )
        }
        if (todos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You have not created any TODOs yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(todos, key = { todo -> todo.id }) { todo ->
                    TodoItem(todo = todo, onLongPress = { showDialog = todo }) { isChecked ->
                        todoViewModel.onTodoCompleted(todo, isChecked)
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

@Composable
private fun FavoriteRewardsProgress(
    favoriteRewards: List<Reward>,
    currentCoins: Int
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text("Favorite Rewards Progress", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Purple40)
            Spacer(modifier = Modifier.height(8.dp))
            favoriteRewards.sortedByDescending { (currentCoins.toFloat() / it.cost).coerceAtMost(1f) }
                .forEach { reward ->
                    val progress = (currentCoins.toFloat() / reward.cost).coerceAtMost(1f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(reward.title, modifier = Modifier.weight(1f), color = Purple40)
                        Text("${(progress * 100).toInt()}%/${reward.cost}", color = Purple40)
                    }
                    LinearProgressIndicator(
                        progress = { (currentCoins.toFloat() / reward.cost).coerceAtMost(1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Purple40
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
        }
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
