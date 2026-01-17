package com.nobadhabbits.cornytask.features.time_goals

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nobadhabbits.cornytask.data.TimeGoal

@Composable
fun TimeGoalsScreen(viewModel: TimeGoalsViewModel = viewModel(), navController: NavController) {
    val timeGoals by viewModel.timeGoals.collectAsState()
    val activeTimeGoal by viewModel.activeTimeGoal.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<TimeGoal?>(null) }
    val context = LocalContext.current

    LaunchedEffect(activeTimeGoal) {
        if (activeTimeGoal != null) {
            navController.navigate("timer_screen")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.isAddingTimeGoal = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add a new time goal")
            }
        }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            items(timeGoals) { timeGoal ->
                key(timeGoal.id) {
                    val isActive = activeTimeGoal?.id == timeGoal.id
                    TimeGoalItem(
                        timeGoal = timeGoal,
                        isActive = isActive,
                        onStartGoal = { viewModel.onStartGoalClicked(timeGoal) },
                        onActiveClick = { navController.navigate("timer_screen") },
                        onLongPress = { showDeleteDialog = timeGoal }
                    )
                }
            }
        }
    }

    if (viewModel.isAddingTimeGoal) {
        AddGoalDialog(
            title = viewModel.newTimeGoalTitle,
            onTitleChange = { viewModel.newTimeGoalTitle = it },
            hours = viewModel.newTimeGoalHours,
            onHoursChange = { viewModel.newTimeGoalHours = it },
            minutes = viewModel.newTimeGoalMinutes,
            onMinutesChange = { viewModel.newTimeGoalMinutes = it },
            rewardCoins = viewModel.newTimeGoalRewardCoins,
            onRewardCoinsChange = { viewModel.newTimeGoalRewardCoins = it },
            onAdd = { viewModel.onAddTimeGoal() },
            onCancel = { viewModel.isAddingTimeGoal = false }
        )
    }

    if (viewModel.showDurationDialog) {
        viewModel.selectedTimeGoal?.let { goal ->
            DurationSelectionDialog(
                timeGoal = goal,
                onDismiss = { viewModel.showDurationDialog = false },
                onStart = { duration -> viewModel.onStartTimer(context, duration) }
            )
        }
    }

    showDeleteDialog?.let { timeGoal ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Time Goal") },
            text = { Text("Are you sure you want to delete this time goal?") },
            confirmButton = {
                TextButton(onClick = { viewModel.onDeleteTimeGoal(timeGoal); showDeleteDialog = null }) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TimeGoalItem(
    timeGoal: TimeGoal,
    isActive: Boolean,
    onStartGoal: () -> Unit,
    onActiveClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val progress = if (timeGoal.totalTimeMinutes > 0) {
        (timeGoal.totalTimeMinutes - timeGoal.remainingTimeMinutes).toFloat() / timeGoal.totalTimeMinutes
    } else {
        0f
    }

    ListItem(
        headlineContent = {
            Text(
                text = timeGoal.title,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        },
        supportingContent = {
            Column {
                Text("Remaining: ${formatTime(timeGoal.remainingTimeMinutes)} of ${formatTime(timeGoal.totalTimeMinutes)}")
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        trailingContent = {
            if (isActive) {
                Button(onClick = onActiveClick) {
                    Text("Active", color = Color.Green, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = onStartGoal, enabled = timeGoal.remainingTimeMinutes > 0) {
                    Text(if (timeGoal.remainingTimeMinutes > 0) "Start Goal" else "Completed")
                }
            }
        },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { onLongPress() })
        }
    )
}

@Composable
private fun AddGoalDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    hours: String,
    onHoursChange: (String) -> Unit,
    minutes: String,
    onMinutesChange: (String) -> Unit,
    rewardCoins: String,
    onRewardCoinsChange: (String) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add New Time Goal") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = hours,
                        onValueChange = onHoursChange,
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = minutes,
                        onValueChange = onMinutesChange,
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = rewardCoins,
                    onValueChange = onRewardCoinsChange,
                    label = { Text("Reward Coins") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onAdd) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DurationSelectionDialog(
    timeGoal: TimeGoal,
    onDismiss: () -> Unit,
    onStart: (Long) -> Unit
) {
    val remainingHours = (timeGoal.remainingTimeMinutes / 60).toString()
    val remainingMinutes = (timeGoal.remainingTimeMinutes % 60).toString()
    var hours by remember { mutableStateOf(remainingHours) }
    var minutes by remember { mutableStateOf(remainingMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Timer") },
        text = {
            Column {
                Text("Enter duration for ${timeGoal.title}.")
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = minutes,
                        onValueChange = { minutes = it },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val h = hours.toLongOrNull() ?: 0L
                val m = minutes.toLongOrNull() ?: 0L
                val durationMinutes = (h * 60) + m
                if (durationMinutes > 0) {
                    onStart(durationMinutes)
                }
            }) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(totalMinutes: Long): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}