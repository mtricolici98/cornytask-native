package com.nobadhabbits.cornytask.features.time_goals

import com.nobadhabbits.cornytask.data.TimeGoal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class TimeGoalRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val timeGoalsCollection
        get() = firestore.collection("users").document(auth.currentUser!!.uid).collection("timeGoals")

    fun  getTimeGoalsFlow(): Flow<List<TimeGoal>> = callbackFlow {
        val subscription = timeGoalsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val timeGoals = snapshot.toObjects(TimeGoal::class.java)
                    trySend(timeGoals)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun addTimeGoal(title: String, totalTimeMinutes: Long, rewardCoins: Int) {
        val newTimeGoal = TimeGoal(
            id = timeGoalsCollection.document().id,
            title = title,
            totalTimeMinutes = totalTimeMinutes,
            remainingTimeMinutes = totalTimeMinutes,
            rewardCoins = rewardCoins
        )
        timeGoalsCollection.document(newTimeGoal.id).set(newTimeGoal)
    }

    fun updateTimeGoal(timeGoal: TimeGoal) {
        timeGoalsCollection.document(timeGoal.id).set(timeGoal)
    }


    fun goalRef(goalId: String): DocumentReference =
        timeGoalsCollection.document(goalId)

    fun deleteTimeGoal(timeGoalId: String) {
        timeGoalsCollection.document(timeGoalId).delete()
    }
}
