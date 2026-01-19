package com.nobadhabbits.cornytask.features.time_goals

import com.nobadhabbits.cornytask.data.History
import com.nobadhabbits.cornytask.data.TimeGoal
import com.nobadhabbits.cornytask.features.history.HistoryRepository
import com.nobadhabbits.cornytask.features.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimeTrackingRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val timeGoalRepository = TimeGoalRepository()
    private val historyRepository = HistoryRepository()
    private val userRepository = UserRepository()

    fun applyElapsedTime(timeGoal: TimeGoal, elapsedMinutes: Long) {
        repositoryScope.launch {
            val wasCompleted = timeGoal.remainingTimeMinutes <= 0
            val newRemainingTime = (timeGoal.remainingTimeMinutes - elapsedMinutes).coerceAtLeast(0)

            val updatedGoal = timeGoal.copy(remainingTimeMinutes = newRemainingTime)
            timeGoalRepository.updateTimeGoal(updatedGoal)

            if (elapsedMinutes > 0) {
                val historyEntry = History(
                    title = "Tracked time for '${timeGoal.title}'",
                    rewardCoins = 0,
                    type = "TIME_GOAL",
                    durationMinutes = elapsedMinutes,
                    timeGoalId = timeGoal.id
                )
                historyRepository.addHistoryEntry(historyEntry)
            }

            val isNowCompleted = newRemainingTime <= 0
            if (!wasCompleted && isNowCompleted) {
                userRepository.addCoins(timeGoal.rewardCoins)
                val completionHistory = History(
                    title = "Completed: ${timeGoal.title}",
                    rewardCoins = timeGoal.rewardCoins,
                    type = "TIME_GOAL",
                    timeGoalId = timeGoal.id
                )
                historyRepository.addHistoryEntry(completionHistory)
                userRepository.clearActiveTimeGoal()
            }
        }
    }
}
