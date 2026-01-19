package com.nobadhabbits.cornytask.features.time_goals;

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nobadhabbits.cornytask.data.History
import com.nobadhabbits.cornytask.data.TimeGoal
import com.nobadhabbits.cornytask.features.history.HistoryRepository

class TimeTrackingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val timeGoalRepository = TimeGoalRepository()
    private val historyRepository = HistoryRepository()

    fun applyElapsedTime(timeGoal: TimeGoal, elapsedMinutes: Long) {
        val goalRef = timeGoalRepository.goalRef(timeGoal.id);
        val historyRef = historyRepository.newHistoryRef();

        val historyEntry = History(
            title = timeGoal.title,
            rewardCoins = if (timeGoal.remainingTimeMinutes - elapsedMinutes > 0) 0 else timeGoal.rewardCoins,
            type = "TIME_GOAL",
            durationMinutes = elapsedMinutes,
            timeGoalId = timeGoal.id
        )
        if (historyRef != null) firestore.runBatch { batch ->
            // Atomic-ish offline: both writes are queued together
            batch.update(goalRef, "remainingTimeMinutes", FieldValue.increment(-elapsedMinutes))
            batch.set(historyRef, historyEntry);
        }
    }
}
