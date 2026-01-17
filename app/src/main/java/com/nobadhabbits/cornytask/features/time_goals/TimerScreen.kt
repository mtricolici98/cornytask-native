package com.nobadhabbits.cornytask.features.time_goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.util.concurrent.TimeUnit

@Composable
fun TimerScreen(viewModel: TimeGoalsViewModel = viewModel(), navController: NavController) {
    val timerState by TimeGoalManager.timerState.collectAsState()
    val activeTimeGoal by viewModel.activeTimeGoal.collectAsState()
    val context = LocalContext.current

    var previouslyActiveGoal = remember { activeTimeGoal }

    LaunchedEffect(activeTimeGoal) {
        if (activeTimeGoal == null) {
            navController.popBackStack()
        }
    }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (timerState is TimeGoalManager.TimerState.Running) {
                val runningState = timerState as TimeGoalManager.TimerState.Running
                val remainingMillis = runningState.remainingMillis

                val timeString = if (remainingMillis >= TimeUnit.HOURS.toMillis(1)) {
                    val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
                    "%02dh:%02dm left...".format(hours, minutes)
                } else {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
                    "%02dm:%02ds left...".format(minutes, seconds)
                }

                val goal = runningState.goal
                previouslyActiveGoal = goal
                Text(text = goal.title, modifier = Modifier.padding(bottom = 16.dp), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.size(16.dp))
                Text(text = timeString, modifier = Modifier.padding(bottom = 8.dp))
                Button(onClick = { viewModel.onStopTimer(context) }) {
                    Text("Stop Timer")
                }
            } else {
                if (previouslyActiveGoal != null) {
                    Text(text = previouslyActiveGoal!!.title, modifier = Modifier.padding(bottom = 16.dp), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(text = "Time goal complete", modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
}
