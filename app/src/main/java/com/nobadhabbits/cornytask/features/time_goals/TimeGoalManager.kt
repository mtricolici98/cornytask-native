package com.nobadhabbits.cornytask.features.time_goals

import android.os.CountDownTimer
import com.nobadhabbits.cornytask.data.History
import com.nobadhabbits.cornytask.data.TimeGoal
import com.nobadhabbits.cornytask.features.history.HistoryRepository
import com.nobadhabbits.cornytask.features.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object TimeGoalManager {
    sealed class TimerState {
        object Idle : TimerState()
        data class Running(val goal: TimeGoal, val remainingMillis: Long, val totalDurationMillis: Long) : TimerState()
        data class Finished(val goal: TimeGoal, val totalDurationMillis: Long) : TimerState()
    }

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState

    private var countDownTimer: CountDownTimer? = null
    private val timeGoalRepository = TimeGoalRepository()
    private val historyRepository = HistoryRepository()
    private val userRepository = UserRepository()
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var totalDurationMillisForCurrentTimer: Long = 0L
    private val timeTrackingRepository = TimeTrackingRepository()

    fun startTimer(timeGoal: TimeGoal, durationMillis: Long) {
        mainScope.launch {
            countDownTimer?.cancel()
            totalDurationMillisForCurrentTimer = durationMillis
            _timerState.value = TimerState.Running(timeGoal, durationMillis, durationMillis)

            countDownTimer = object : CountDownTimer(durationMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    _timerState.value = TimerState.Running(timeGoal, millisUntilFinished, durationMillis)
                }

                override fun onFinish() {
                    _timerState.value = TimerState.Finished(timeGoal, totalDurationMillisForCurrentTimer)
                    handleFinish(timeGoal, totalDurationMillisForCurrentTimer)
                }
            }.start()
        }
    }

    fun stopTimer() {
        mainScope.launch {
            val currentState = _timerState.value
            if (currentState is TimerState.Running) {
                countDownTimer?.cancel()
                val elapsedMillis = totalDurationMillisForCurrentTimer - currentState.remainingMillis
                handleStop(currentState.goal.id, elapsedMillis)
                _timerState.value = TimerState.Idle
            }
        }
    }

    private fun handleStop(goalId: String, elapsedMillis: Long) {
        ioScope.launch {
            val timeGoal = timeGoalRepository.getTimeGoalsFlow().first().find { it.id == goalId } ?: return@launch
            val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)
            if (elapsedMinutes > 0) {
                timeTrackingRepository.applyElapsedTime(timeGoal, elapsedMinutes)
            }
        }
    }

    private fun handleFinish(timeGoal: TimeGoal, durationMillis: Long) {
        ioScope.launch {

            val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
            timeTrackingRepository.applyElapsedTime(timeGoal, durationMinutes);
            val newRemainingTime = timeGoal.remainingTimeMinutes - durationMinutes
            if (newRemainingTime <= 0) {
                userRepository.addCoins(timeGoal.rewardCoins)
            }
        }
    }
}
