package com.nobadhabbits.cornytask.features.time_goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nobadhabbits.cornytask.data.TimeGoal
import com.nobadhabbits.cornytask.features.main.Routes
import java.util.concurrent.TimeUnit

private val SessionPresetsMinutes = listOf(15L, 25L, 45L, 60L)

@Composable
fun TimeGoalsScreen(viewModel: TimeGoalsViewModel = viewModel(), navController: NavController) {
    val timeGoals by viewModel.timeGoals.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val running = timerState as? TimeGoalManager.TimerState.Running
    var isAdding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TimeGoal?>(null) }
    var starting by remember { mutableStateOf<TimeGoal?>(null) }
    var showDeleteDialog by remember { mutableStateOf<TimeGoal?>(null) }
    var showOptionsDialog by remember { mutableStateOf<TimeGoal?>(null) }
    var showManualInputDialog by remember { mutableStateOf<TimeGoal?>(null) }
    val context = LocalContext.current
    val openTimer = { navController.navigate(Routes.TIMER) { launchSingleTop = true } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { isAdding = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add a new time goal")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            if (running != null) {
                item(key = "running") {
                    RunningSessionCard(
                        state = running,
                        onOpen = openTimer,
                        onStop = { viewModel.onStopTimer(context) }
                    )
                }
            }
            if (timeGoals.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No time goals yet. Tap + to add one.")
                    }
                }
            }
            items(timeGoals, key = { it.id }) { timeGoal ->
                TimeGoalItem(
                    timeGoal = timeGoal,
                    isActive = running?.goal?.id == timeGoal.id,
                    canStart = running == null,
                    onStart = { starting = timeGoal },
                    onOpenTimer = openTimer,
                    onRestart = { viewModel.onRestartTimeGoal(timeGoal) },
                    onLongPress = { showOptionsDialog = timeGoal }
                )
            }
        }
    }

    if (isAdding) {
        GoalFormDialog(
            dialogTitle = "Add New Time Goal",
            confirmLabel = "Add",
            initial = null,
            onSave = { title, minutes, coins ->
                viewModel.onAddTimeGoal(title, minutes, coins)
                isAdding = false
            },
            onCancel = { isAdding = false }
        )
    }

    editing?.let { timeGoal ->
        GoalFormDialog(
            dialogTitle = "Edit Time Goal",
            confirmLabel = "Save",
            initial = timeGoal,
            onSave = { title, minutes, coins ->
                viewModel.onEditTimeGoal(timeGoal, title, minutes, coins)
                editing = null
            },
            onCancel = { editing = null }
        )
    }

    starting?.let { timeGoal ->
        StartSessionDialog(
            timeGoal = timeGoal,
            onStart = { minutes ->
                viewModel.onStartTimer(context, timeGoal, minutes)
                starting = null
            },
            onDismiss = { starting = null }
        )
    }

    showOptionsDialog?.let { timeGoal ->
        val isActive = running?.goal?.id == timeGoal.id
        val hasProgress = timeGoal.remainingTimeMinutes < timeGoal.totalTimeMinutes
        AlertDialog(
            onDismissRequest = { showOptionsDialog = null },
            title = { Text(timeGoal.title) },
            text = {
                Column {
                    OptionRow(Icons.Default.Edit, "Edit") {
                        showOptionsDialog = null
                        editing = timeGoal
                    }
                    OptionRow(Icons.Default.MoreTime, "Add time manually") {
                        showOptionsDialog = null
                        showManualInputDialog = timeGoal
                    }
                    // Changing a goal under a running timer would get overwritten when it stops.
                    if (!isActive && hasProgress) {
                        OptionRow(Icons.Default.Replay, "Start over from zero") {
                            showOptionsDialog = null
                            viewModel.onRestartTimeGoal(timeGoal)
                        }
                    }
                    if (!isActive) {
                        OptionRow(Icons.Default.Delete, "Delete") {
                            showOptionsDialog = null
                            showDeleteDialog = timeGoal
                        }
                    } else {
                        Text(
                            "Stop the timer to delete or restart this goal.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptionsDialog = null }) { Text("Close") }
            }
        )
    }

    showManualInputDialog?.let { timeGoal ->
        ManualTimeInputDialog(
            timeGoal = timeGoal,
            onDismiss = { showManualInputDialog = null },
            onAddTime = { hours, minutes ->
                viewModel.onAddManualTime(timeGoal, hours, minutes)
            }
        )
    }

    showDeleteDialog?.let { timeGoal ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Time Goal") },
            text = { Text("Are you sure you want to delete this time goal?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeleteTimeGoal(timeGoal)
                    showDeleteDialog = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RunningSessionCard(
    state: TimeGoalManager.TimerState.Running,
    onOpen: () -> Unit,
    onStop: () -> Unit
) {
    val progress = if (state.totalDurationMillis > 0) {
        1f - state.remainingMillis.toFloat() / state.totalDurationMillis
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Running now", style = MaterialTheme.typography.labelMedium)
            Text(state.goal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(formatCountdown(state.remainingMillis) + " left in this session")
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onOpen) { Text("Open timer") }
                OutlinedButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Stop & save")
                }
            }
        }
    }
}

@Composable
private fun TimeGoalItem(
    timeGoal: TimeGoal,
    isActive: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onOpenTimer: () -> Unit,
    onRestart: () -> Unit,
    onLongPress: () -> Unit
) {
    val progress = if (timeGoal.totalTimeMinutes > 0) {
        (timeGoal.totalTimeMinutes - timeGoal.remainingTimeMinutes).toFloat() / timeGoal.totalTimeMinutes
    } else {
        0f
    }
    val completed = timeGoal.remainingTimeMinutes <= 0

    ListItem(
        headlineContent = {
            Text(
                text = timeGoal.title,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        },
        supportingContent = {
            Column {
                Text(
                    if (completed) "Done! ${formatTime(timeGoal.totalTimeMinutes)} · ${timeGoal.rewardCoins} coins earned"
                    else "Remaining: ${formatTime(timeGoal.remainingTimeMinutes)} of ${formatTime(timeGoal.totalTimeMinutes)} · ${timeGoal.rewardCoins} coins"
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        trailingContent = {
            when {
                isActive -> FilledTonalButton(onClick = onOpenTimer) { Text("Running") }
                completed -> OutlinedButton(onClick = onRestart) { Text("Again") }
                else -> Button(onClick = onStart, enabled = canStart) { Text("Start") }
            }
        },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { onLongPress() })
        }
    )
}

@Composable
private fun OptionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** Pick how long to work on the goal right now: a preset, everything that's left, or custom. */
@Composable
private fun StartSessionDialog(
    timeGoal: TimeGoal,
    onStart: (minutes: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val remaining = timeGoal.remainingTimeMinutes
    val presets = SessionPresetsMinutes.filter { it < remaining } + remaining
    var selected by remember { mutableStateOf(remaining) }
    var custom by remember { mutableStateOf("") }
    val customMinutes = custom.toLongOrNull()
    val minutes = if (custom.isNotBlank()) customMinutes else selected
    val valid = minutes != null && minutes in 1..remaining

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start ${timeGoal.title}") },
        text = {
            Column {
                Text("How long do you want to go for?")
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = custom.isBlank() && selected == preset,
                            onClick = {
                                selected = preset
                                custom = ""
                            },
                            label = {
                                Text(if (preset == remaining) "All (${formatTime(preset)})" else formatTime(preset))
                            }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = custom,
                    onValueChange = { custom = it.filter(Char::isDigit) },
                    label = { Text("Or custom minutes") },
                    supportingText = if (custom.isNotBlank() && !valid) {
                        { Text("Between 1 and $remaining minutes") }
                    } else null,
                    isError = custom.isNotBlank() && !valid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { minutes?.let(onStart) }, enabled = valid) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun GoalFormDialog(
    dialogTitle: String,
    confirmLabel: String,
    initial: TimeGoal?,
    onSave: (title: String, totalMinutes: Long, rewardCoins: Int) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var hours by remember { mutableStateOf(initial?.let { (it.totalTimeMinutes / 60).takeIf { h -> h > 0 }?.toString() }.orEmpty()) }
    var minutes by remember { mutableStateOf(initial?.let { (it.totalTimeMinutes % 60).takeIf { m -> m > 0 }?.toString() }.orEmpty()) }
    var rewardCoins by remember { mutableStateOf(initial?.rewardCoins?.toString().orEmpty()) }

    val totalMinutes = (hours.toLongOrNull() ?: 0L) * 60 + (minutes.toLongOrNull() ?: 0L)
    val coins = rewardCoins.toIntOrNull()
    val valid = title.isNotBlank() && totalMinutes > 0 && coins != null && coins >= 0

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(dialogTitle) },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = hours,
                        onValueChange = { hours = it.filter(Char::isDigit) },
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = minutes,
                        onValueChange = { minutes = it.filter(Char::isDigit) },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = rewardCoins,
                    onValueChange = { rewardCoins = it.filter(Char::isDigit) },
                    label = { Text("Reward Coins") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                initial?.let {
                    val done = it.totalTimeMinutes - it.remainingTimeMinutes
                    if (done > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Already done: ${formatTime(done)}. That time is kept.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title.trim(), totalMinutes, coins ?: 0) }, enabled = valid) {
                Text(confirmLabel)
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
private fun ManualTimeInputDialog(
    timeGoal: TimeGoal,
    onDismiss: () -> Unit,
    onAddTime: (hours: String, minutes: String) -> Unit
) {
    var manualHours by remember { mutableStateOf("") }
    var manualMinutes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Time Input") },
        text = {
            Column {
                Text("Add time for ${timeGoal.title}")
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = manualHours,
                        onValueChange = { manualHours = it },
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextField(
                        value = manualMinutes,
                        onValueChange = { manualMinutes = it },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onAddTime(manualHours, manualMinutes)
                onDismiss()
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

internal fun formatTime(totalMinutes: Long): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

/** 1:05:09 or 05:09 */
internal fun formatCountdown(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
