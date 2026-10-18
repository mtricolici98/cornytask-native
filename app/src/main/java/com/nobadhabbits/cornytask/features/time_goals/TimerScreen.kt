package com.nobadhabbits.cornytask.features.time_goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun TimerScreen(viewModel: TimeGoalsViewModel = viewModel(), navController: NavController) {
    val timerState by viewModel.timerState.collectAsState()
    val context = LocalContext.current
    // Only leave on our own when a timer we were showing stops. Arriving while idle
    // (e.g. from a notification while the app is still restoring) shouldn't bounce you out.
    var sawRunning by remember { mutableStateOf(false) }

    LaunchedEffect(timerState) {
        when (timerState) {
            is TimeGoalManager.TimerState.Running -> sawRunning = true
            TimeGoalManager.TimerState.Idle -> if (sawRunning) navController.popBackStack()
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (val state = timerState) {
            is TimeGoalManager.TimerState.Running -> {
                val progress = if (state.totalDurationMillis > 0) {
                    1f - state.remainingMillis.toFloat() / state.totalDurationMillis
                } else 0f
                Text(state.goal.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(32.dp))
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(220.dp),
                        strokeWidth = 10.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatCountdown(state.remainingMillis), style = MaterialTheme.typography.displaySmall)
                        Text(
                            "of ${formatTime(state.totalDurationMillis / 60_000)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    "Stopping early still counts the minutes you did.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { viewModel.onStopTimer(context) }) {
                    Text("Stop & save")
                }
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Back to goals (keeps running)")
                }
            }

            is TimeGoalManager.TimerState.Finished -> {
                Text(state.goal.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text("Session complete: ${formatTime(state.totalDurationMillis / 60_000)}. Well done!")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) { Text("Back to goals") }
            }

            TimeGoalManager.TimerState.Idle -> {
                Text("No timer running right now.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) { Text("Back to goals") }
            }
        }
    }
}
