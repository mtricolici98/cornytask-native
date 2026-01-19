package com.nobadhabbits.cornytask.features.time_goals

import android.os.CountDownTimer
import com.nobadhabbits.cornytask.data.TimeGoal
import com.nobadhabbits.cornytask.features.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val userRepository = UserRepository()
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var totalDurationMillisForCurrentTimer: Long = 0L
    private val timeTrackingRepository = TimeTrackingRepository()

    init {
        managerScope.launch {
            userRepository.getUserFlow().collectLatest { user ->
                val activeGoalId = user?.activeTimeGoalId
                val startTimeMillis = user?.activeTimeGoalStartTimeMillis

                if (activeGoalId != null && startTimeMillis != null) {
                    val currentTimerGoalId = (timerState.value as? TimerState.Running)?.goal?.id
                    if (activeGoalId != currentTimerGoalId) {
                        val goal = timeGoalRepository.getTimeGoal(activeGoalId)
                        if (goal != null) {
                            val elapsedMillis = System.currentTimeMillis() - startTimeMillis
                            val remainingMillis = (goal.totalTimeMinutes * 60 * 1000) - elapsedMillis
                            if (remainingMillis > 0) {
                                startTimer(goal, remainingMillis)
                            } else {
                                handleFinish(goal, goal.totalTimeMinutes * 60 * 1000)
                                userRepository.clearActiveTimeGoal()
                            }
                        }
                    }
                } else {
                    if (timerState.value is TimerState.Running || timerState.value is TimerState.Finished) {
                        stopTimer()
                    }
                }
            }
        }
    }

    fun startTimer(timeGoal: TimeGoal, durationMillis: Long) {
        managerScope.launch {
            countDownTimer?.cancel()
            totalDurationMillisForCurrentTimer = durationMillis
            _timerState.value = TimerState.Running(timeGoal, durationMillis, durationMillis)

            val startTimeMillis = System.currentTimeMillis()
            userRepository.setActiveTimeGoal(timeGoal.id, startTimeMillis)

            countDownTimer = object : CountDownTimer(durationMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    _timerState.value = TimerState.Running(timeGoal, millisUntilFinished, durationMillis)
                }

                override fun onFinish() {
                    _timerState.value = TimerState.Finished(timeGoal, totalDurationMillisForCurrentTimer)
                    stopTimer()
                }
            }.start()
        }
    }

    fun stopTimer() {
        managerScope.launch {
            val currentState = _timerState.value
            val runningState = when (currentState) {
                is TimerState.Running -> currentState
                is TimerState.Finished -> TimerState.Running(currentState.goal, 0, currentState.totalDurationMillis)
                else -> null
            }

            if (runningState != null) {
                countDownTimer?.cancel()

                val user = userRepository.fetchCurrentUser()
                if (user?.activeTimeGoalId == runningState.goal.id) {
                    userRepository.clearActiveTimeGoal()

                    val startTimeMillis = user.activeTimeGoalStartTimeMillis ?: System.currentTimeMillis()
                    val elapsedMillis = System.currentTimeMillis() - startTimeMillis
                    handleStop(runningState.goal, elapsedMillis)
                }

                if (currentState !is TimerState.Finished) {
                    _timerState.value = TimerState.Idle
                }
            }
        }
    }

    private fun handleStop(timeGoal: TimeGoal, elapsedMillis: Long) {
        val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis)
        if (elapsedMinutes > 0) {
            timeTrackingRepository.applyElapsedTime(timeGoal, elapsedMinutes)
        }
    }

    private fun handleFinish(timeGoal: TimeGoal, durationMillis: Long) {
        val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
        timeTrackingRepository.applyElapsedTime(timeGoal, durationMinutes)
    }
}
