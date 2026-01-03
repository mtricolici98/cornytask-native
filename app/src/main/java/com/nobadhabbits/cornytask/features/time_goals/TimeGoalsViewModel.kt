package com.nobadhabbits.cornytask.features.time_goals

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.data.TimeGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimeGoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val timeGoalRepository = TimeGoalRepository()

    val timeGoals: StateFlow<List<TimeGoal>> = timeGoalRepository.getTimeGoalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTimeGoal: StateFlow<TimeGoal?> = TimeGoalManager.timerState.map {
        when (it) {
            is TimeGoalManager.TimerState.Running -> timeGoals.value.find { goal -> goal.id == it.goalId }
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var isAddingTimeGoal by mutableStateOf(false)
    var newTimeGoalTitle by mutableStateOf("")
    var newTimeGoalTotalTime by mutableStateOf("")
    var newTimeGoalRewardCoins by mutableStateOf("")

    var selectedTimeGoal by mutableStateOf<TimeGoal?>(null)
    var showDurationDialog by mutableStateOf(false)

    fun onStartGoalClicked(timeGoal: TimeGoal) {
        selectedTimeGoal = timeGoal
        showDurationDialog = true
    }

    fun onStartTimer(context: Context, duration: Long) {
        selectedTimeGoal?.let { goal ->
            val intent = Intent(context, TimeGoalService::class.java).apply {
                action = TimeGoalService.ACTION_START
                putExtra(TimeGoalService.EXTRA_TIME_GOAL_ID, goal.id)
            }
            context.startService(intent)
            showDurationDialog = false
        }
    }

    fun onStopTimer(context: Context) {
        context.startService(Intent(context, TimeGoalService::class.java).apply { action = TimeGoalService.ACTION_STOP })
    }

    fun onAddTimeGoal() {
        viewModelScope.launch {
            val totalTime = newTimeGoalTotalTime.toLongOrNull()
            val rewardCoins = newTimeGoalRewardCoins.toIntOrNull()
            if (newTimeGoalTitle.isNotBlank() && totalTime != null && totalTime > 0 && rewardCoins != null && rewardCoins > 0) {
                timeGoalRepository.addTimeGoal(newTimeGoalTitle, totalTime, rewardCoins)
                newTimeGoalTitle = ""
                newTimeGoalTotalTime = ""
                newTimeGoalRewardCoins = ""
                isAddingTimeGoal = false
            }
        }
    }

    fun onDeleteTimeGoal(timeGoal: TimeGoal) {
        viewModelScope.launch {
            timeGoalRepository.deleteTimeGoal(timeGoal.id)
        }
    }
}
