package com.nobadhabbits.cornytask.features.rewards

import com.nobadhabbits.cornytask.data.Reward
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RewardRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getRewardsFlow(): Flow<List<Reward>> = callbackFlow {
        var snapshotListener: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            snapshotListener?.remove()

            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(emptyList())
            } else {
                val collection = firestore.collection("users").document(user.uid).collection("rewards")
                snapshotListener = collection
                    .orderBy("cost", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val rewards = snapshot?.documents?.mapNotNull {
                            it.toObject(Reward::class.java)?.copy(id = it.id)
                        } ?: emptyList()

                        trySend(rewards)
                    }
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            snapshotListener?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    suspend fun addReward(title: String, cost: Int) {
        val uid = auth.currentUser?.uid ?: return
        val reward = Reward(title = title, cost = cost)
        firestore.collection("users").document(uid).collection("rewards").add(reward).await()
    }

    suspend fun deleteReward(rewardId: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("rewards").document(rewardId).delete().await()
    }

    suspend fun updateReward(reward: Reward) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("rewards").document(reward.id).set(reward).await()
    }
}
