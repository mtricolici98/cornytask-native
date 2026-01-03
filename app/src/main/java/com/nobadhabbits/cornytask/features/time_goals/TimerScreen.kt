package com.nobadhabbits.cornytask.features.time_goals

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.util.concurrent.TimeUnit

@Composable
fun TimerScreen(viewModel: TimeGoalsViewModel = viewModel(), navController: NavController) {
    val context = LocalContext.current
    val timerState by TimeGoalManager.timerState.collectAsState()
    val activeTimeGoal by viewModel.activeTimeGoal.collectAsState()

    LaunchedEffect(timerState) {
        if (timerState is TimeGoalManager.TimerState.Idle || timerState is TimeGoalManager.TimerState.Finished) {
            navController.popBackStack()
        }
    }

    val remainingMillis = (timerState as? TimeGoalManager.TimerState.Running)?.remainingMillis ?: 0
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        activeTimeGoal?.let {
            Text(text = it.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(text = timeString, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.onStopTimer(context) }) {
            Text("Stop")
        }
    }
}
