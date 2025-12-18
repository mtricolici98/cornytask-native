package com.example.cornytask_v2.features.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimerScreen() {
    val context = LocalContext.current
    var timerService by remember { mutableStateOf<TimerService?>(null) }
    val isRunning by timerService?.isRunning?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }
    val timeMillis by timerService?.timeMillis?.collectAsState(initial = 0L) ?: remember { mutableStateOf(0L) }
    var showTimePicker by remember { mutableStateOf(false) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as TimerService.TimerBinder
                timerService = binder.getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                timerService = null
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { hours, minutes ->
                val newInitialTime = (hours * 3600 + minutes * 60) * 1000L
                timerService?.setInitialTime(newInitialTime)
                showTimePicker = false
            }
        )
    }

    DisposableEffect(Unit) {
        Intent(context, TimerService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        onDispose {
            context.unbindService(connection)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = formatTime(timeMillis),
            fontSize = 72.sp,
            modifier = Modifier.clickable { showTimePicker = true }
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row {
            Button(onClick = { timerService?.toggle() }) {
                Icon(
                    if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Play"
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { timerService?.reset() }) {
                Icon(Icons.Filled.RestartAlt, contentDescription = "Reset")
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60)) % 24
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Timer") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            Button(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}