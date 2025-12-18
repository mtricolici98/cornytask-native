package com.example.cornytask_v2.features.todo

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTodoScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val viewModel: EditTodoViewModel = viewModel(
        factory = EditTodoViewModelFactory(context.applicationContext as Application)
    )

    val todoId = (context as EditTodoActivity).intent.getStringExtra("todoId")

    LaunchedEffect(todoId) {
        todoId?.let {
            viewModel.loadTodo(it)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit To-do") }) }
    ) {
        Column(modifier = Modifier.padding(it).padding(16.dp)) {
            TextField(
                value = viewModel.title,
                onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = viewModel.rewardCoins,
                onValueChange = { viewModel.rewardCoins = it },
                label = { Text("Reward in Unicorns") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.onUpdateTodo(onSuccess = onNavigateUp) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update To-do")
            }
        }
    }
}
