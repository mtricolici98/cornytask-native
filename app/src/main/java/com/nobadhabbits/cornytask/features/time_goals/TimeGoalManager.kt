package com.nobadhabbits.cornytask.features.time_goals

import android.os.CountDownTimer
import com.nobadhabbits.cornytask.data.History
import com.nobadhabbits.cornytask.data.TimeGoal
import com.nobadhabbits.cornytask.features.history.HistoryRepository
import com.nobadhabbits.cornytask.features.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object TimeGoalManager {
    sealed class TimerState {
        object Idle : TimerState()
        data class Running(val goalId: String, val remainingMillis: Long) : TimerState()
        data class Finished(val goalId: String) : TimerState()
    }

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState

    private var countDownTimer: CountDownTimer? = null
    private val timeGoalRepository = TimeGoalRepository()
    private val historyRepository = HistoryRepository()
    private val userRepository = UserRepository()
    private val managerScope = CoroutineScope(Dispatchers.IO)
    private var totalDurationMillisForCurrentTimer: Long = 0L

    fun startTimer(timeGoal: TimeGoal, durationMillis: Long) {
        countDownTimer?.cancel()
        totalDurationMillisForCurrentTimer = durationMillis
        _timerState.value = TimerState.Running(timeGoal.id, durationMillis)

        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _timerState.value = TimerState.Running(timeGoal.id, millisUntilFinished)
            }

            override fun onFinish() {
                _timerState.value = TimerState.Finished(timeGoal.id)
                handleFinish(timeGoal, totalDurationMillisForCurrentTimer)
            }
        }.start()
    }

    fun stopTimer() {
        val currentState = _timerState.value
        if (currentState is TimerState.Running) {
            countDownTimer?.cancel()
            val elapsedMillis = totalDurationMillisForCurrentTimer - currentState.remainingMillis
            handleStop(currentState.goalId, elapsedMillis)
            _timerState.value = TimerState.Idle
        }
    }

    private fun handleStop(goalId: String, elapsedMillis: Long) {
        managerScope.launch {
            val timeGoal = timeGoalRepository.getTimeGoalsFlow().first().find { it.id == goalId } ?: return@launch
            val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)
            if (elapsedMinutes > 0) {
                val newRemainingTime = timeGoal.remainingTimeMinutes - elapsedMinutes
                val updatedGoal = timeGoal.copy(remainingTimeMinutes = if (newRemainingTime > 0) newRemainingTime else 0)
                timeGoalRepository.updateTimeGoal(updatedGoal)

                val historyEntry = History(
                    title = timeGoal.title,
                    rewardCoins = 0,
                    type = "TIME_GOAL",
                    durationMinutes = elapsedMinutes,
                    timeGoalId = timeGoal.id
                )
                historyRepository.addHistoryEntry(historyEntry)
            }
        }
    }

    private fun handleFinish(timeGoal: TimeGoal, durationMillis: Long) {
        managerScope.launch {
            val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
            val newRemainingTime = timeGoal.remainingTimeMinutes - durationMinutes
            val updatedGoal = timeGoal.copy(remainingTimeMinutes = if (newRemainingTime > 0) newRemainingTime else 0)
            timeGoalRepository.updateTimeGoal(updatedGoal)

            val historyEntry = History(
                title = timeGoal.title,
                rewardCoins = if (updatedGoal.remainingTimeMinutes <= 0) timeGoal.rewardCoins else 0,
                type = "TIME_GOAL",
                durationMinutes = durationMinutes,
                timeGoalId = timeGoal.id
            )
            historyRepository.addHistoryEntry(historyEntry)

            if (updatedGoal.remainingTimeMinutes <= 0) {
                userRepository.addCoins(timeGoal.rewardCoins)
            }
        }
    }
}
