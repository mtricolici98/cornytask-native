package com.nobadhabbits.cornytask.features.mood_tracking

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nobadhabbits.cornytask.features.mood_tracking.data.MoodRecord
import com.nobadhabbits.cornytask.features.mood_tracking.data.TimeOfDay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date

class MoodTrackingRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {


    private fun moodsCollection() = auth.currentUser?.uid?.let {
        firestore.collection("users").document(it).collection("moods")
    }

    suspend fun addMoodRecord(date: Date, moodScore: Int) {
        val userId = auth.currentUser?.uid ?: return
        val moodRecord = MoodRecord(
            timestamp = date,
            moodScore = moodScore,
            userId = userId
        )
        moodsCollection()?.add(moodRecord)
    }

    fun getMoodRecords(): Flow<List<MoodRecord>> = callbackFlow {
        val listener = moodsCollection()
            ?.orderBy("timestamp", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notes = snapshot.documents.map {
                        it.toObject(MoodRecord::class.java)!!.copy(id = it.id)
                    }
                    trySend(notes)
                }
            }
        awaitClose { listener?.remove() }
    }
}
