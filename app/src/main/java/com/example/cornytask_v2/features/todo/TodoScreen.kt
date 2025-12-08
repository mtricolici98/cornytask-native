package com.example.cornytask_v2.features.todo

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.cornytask_v2.R
import com.example.cornytask_v2.data.Todo

@Composable
fun TodoScreen(viewModel: TodoViewModel = viewModel(factory = TodoViewModelFactory(LocalContext.current))) {
    val todos by viewModel.todos.collectAsState()
    var showDialog by remember { mutableStateOf<Todo?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (todos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You have not created any TODOs yet.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(todos, key = { todo -> todo.id }) { todo ->
                    TodoItem(todo = todo, onLongPress = { showDialog = todo }) { isChecked ->
                        viewModel.onTodoCompleted(todo, isChecked)
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
                TextButton(onClick = { viewModel.onDeleteTodo(todo); showDialog = null }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onResetTodo(todo); showDialog = null }) {
                    Text("Reset")
                }
            }
        )
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
