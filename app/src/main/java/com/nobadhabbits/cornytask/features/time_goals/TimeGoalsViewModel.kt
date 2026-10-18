package com.nobadhabbits.cornytask.features.time_goals

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.data.TimeGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TimeGoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val timeGoalRepository = TimeGoalRepository()

    val timeGoals: StateFlow<List<TimeGoal>> = timeGoalRepository.getTimeGoalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timerState: StateFlow<TimeGoalManager.TimerState> = TimeGoalManager.timerState

    /** Starts a session of [minutes] (at most what's left of the goal). */
    fun onStartTimer(context: Context, timeGoal: TimeGoal, minutes: Long) {
        val sessionMinutes = minutes.coerceIn(1, timeGoal.remainingTimeMinutes)
        val intent = Intent(context, TimeGoalService::class.java).apply {
            action = TimeGoalService.ACTION_START
            putExtra(TimeGoalService.EXTRA_TIME_GOAL_ID, timeGoal.id)
            putExtra(TimeGoalService.EXTRA_DURATION_MINUTES, sessionMinutes)
        }
        context.startService(intent)
    }

    fun onStopTimer(context: Context) {
        context.startService(Intent(context, TimeGoalService::class.java).apply { action = TimeGoalService.ACTION_STOP })
    }

    fun onAddTimeGoal(title: String, totalMinutes: Long, rewardCoins: Int) {
        timeGoalRepository.addTimeGoal(title, totalMinutes, rewardCoins)
    }

    fun onEditTimeGoal(timeGoal: TimeGoal, title: String, totalMinutes: Long, rewardCoins: Int) {
        timeGoalRepository.editTimeGoal(timeGoal, title, totalMinutes, rewardCoins)
    }

    fun onRestartTimeGoal(timeGoal: TimeGoal) {
        timeGoalRepository.restartTimeGoal(timeGoal)
    }

    fun onDeleteTimeGoal(timeGoal: TimeGoal) {
        timeGoalRepository.deleteTimeGoal(timeGoal.id)
    }

    fun onAddManualTime(timeGoal: TimeGoal, hours: String, minutes: String) {
        val totalManualTime = (hours.toLongOrNull() ?: 0L) * 60 + (minutes.toLongOrNull() ?: 0L)
        if (totalManualTime > 0) {
            timeGoalRepository.addManualTime(timeGoal, totalManualTime)
        }
    }
}
