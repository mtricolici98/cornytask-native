package com.nobadhabbits.cornytask.features.time_goals

import com.nobadhabbits.cornytask.data.History
import com.nobadhabbits.cornytask.data.TimeGoal
import com.nobadhabbits.cornytask.features.history.HistoryRepository
import com.nobadhabbits.cornytask.features.medat.MedatRepository
import com.nobadhabbits.cornytask.features.user.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TimeGoalRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val historyRepository = HistoryRepository()
    private val userRepository = UserRepository()
    private val medatRepository = MedatRepository()

    // Null when signed out, so callers can bail out instead of crashing.
    private fun timeGoalsCollection(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("users").document(uid).collection("timeGoals")
    }

    fun getTimeGoalsFlow(): Flow<List<TimeGoal>> = callbackFlow {
        var subscription: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            subscription?.remove()
            subscription = null

            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(emptyList())
            } else {
                subscription = firestore.collection("users").document(user.uid).collection("timeGoals")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            trySend(snapshot.toObjects(TimeGoal::class.java))
                        }
                    }
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            subscription?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    suspend fun getTimeGoal(goalId: String?): TimeGoal? {
        if (goalId.isNullOrEmpty()) {
            return null
        }
        val collection = timeGoalsCollection() ?: return null
        return try {
            collection.document(goalId).get().await().toObject(TimeGoal::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /** Returns the new goal's id, or null when signed out. */
    fun addTimeGoal(
        title: String,
        totalTimeMinutes: Long,
        rewardCoins: Int,
        medatItemIds: List<String> = emptyList()
    ): String? {
        val collection = timeGoalsCollection() ?: return null
        val newTimeGoal = TimeGoal(
            id = collection.document().id,
            title = title,
            totalTimeMinutes = totalTimeMinutes,
            remainingTimeMinutes = totalTimeMinutes,
            rewardCoins = rewardCoins,
            medatItemIds = medatItemIds
        )
        collection.document(newTimeGoal.id).set(newTimeGoal)
        return newTimeGoal.id
    }

    fun updateTimeGoal(timeGoal: TimeGoal) {
        val collection = timeGoalsCollection() ?: return
        collection.document(timeGoal.id).set(timeGoal)
    }

    /** Changes a goal but keeps the time already done (shrinking the target can finish it). */
    fun editTimeGoal(timeGoal: TimeGoal, title: String, totalTimeMinutes: Long, rewardCoins: Int) {
        val doneMinutes = timeGoal.totalTimeMinutes - timeGoal.remainingTimeMinutes
        val newRemaining = (totalTimeMinutes - doneMinutes).coerceAtLeast(0)
        updateTimeGoal(
            timeGoal.copy(
                title = title,
                totalTimeMinutes = totalTimeMinutes,
                remainingTimeMinutes = newRemaining,
                rewardCoins = rewardCoins
            )
        )
        if (timeGoal.remainingTimeMinutes > 0 && newRemaining <= 0) {
            userRepository.addCoins(rewardCoins)
            medatRepository.setDone(timeGoal.medatItemIds, true)
            historyRepository.addHistoryEntry(
                History(
                    title = "Completed: $title",
                    rewardCoins = rewardCoins,
                    type = "TIME_GOAL",
                    timeGoalId = timeGoal.id
                )
            )
        }
    }

    /** Starts the goal over from zero so it can be earned again (e.g. a weekly goal). */
    fun restartTimeGoal(timeGoal: TimeGoal) {
        updateTimeGoal(timeGoal.copy(remainingTimeMinutes = timeGoal.totalTimeMinutes))
    }

    fun deleteTimeGoal(timeGoalId: String) {
        val collection = timeGoalsCollection() ?: return
        collection.document(timeGoalId).delete()
    }

    fun addManualTime(timeGoal: TimeGoal, manualTimeMinutes: Long) {
        if (auth.currentUser == null) return

        val wasCompleted = timeGoal.remainingTimeMinutes <= 0
        val newRemainingTime = (timeGoal.remainingTimeMinutes - manualTimeMinutes).coerceAtLeast(0)
        val updatedGoal = timeGoal.copy(remainingTimeMinutes = newRemainingTime)
        updateTimeGoal(updatedGoal)

        val historyEntry = History(
            title = "Manually added time for '${timeGoal.title}'",
            rewardCoins = 0,
            type = "TIME_GOAL",
            durationMinutes = manualTimeMinutes,
            timeGoalId = timeGoal.id
        )
        historyRepository.addHistoryEntry(historyEntry)

        val isNowCompleted = newRemainingTime <= 0
        if (!wasCompleted && isNowCompleted) {
            userRepository.addCoins(timeGoal.rewardCoins)
            medatRepository.setDone(timeGoal.medatItemIds, true)
            val completionHistory = History(
                title = "Completed: ${timeGoal.title}",
                rewardCoins = timeGoal.rewardCoins,
                type = "TIME_GOAL",
                timeGoalId = timeGoal.id
            )
            historyRepository.addHistoryEntry(completionHistory)
        }
    }
}
