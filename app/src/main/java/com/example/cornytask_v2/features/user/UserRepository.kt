package com.example.cornytask_v2.features.user

import com.example.cornytask_v2.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getUserFlow(): Flow<User?> = callbackFlow {
        var userListener: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            userListener?.remove() // Clean up old user listener

            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(null) // User signed out
            } else {
                val userDocRef = firestore.collection("users").document(user.uid)
                userListener = userDocRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // User document exists, send it
                        trySend(snapshot.toObject(User::class.java))
                    } else {
                        // First login: User is authenticated but has no document. Create it.
                        // Launch in the flow'''s scope to perform the async operation
                        this.launch {
                            try {
                                val newUser = User(uid = user.uid, coins = 0, firstLogin = true)
                                userDocRef.set(newUser).await()
                                // The listener will fire again automatically with the new data,
                                // but we can send it immediately to be faster.
                                trySend(newUser)
                            } catch (e: Exception) {
                                close(e) // Close flow if user creation fails
                            }
                        }
                    }
                }
            }
        }

        auth.addAuthStateListener(authListener)

        // When the flow is cancelled, remove all listeners
        awaitClose {
            userListener?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }


    suspend fun addCoins(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        val userDoc = firestore.collection("users").document(uid)
        firestore.runTransaction {
            val snapshot = it.get(userDoc)
            val newCoins = (snapshot.getLong("coins") ?: 0) + amount
            it.update(userDoc, "coins", newCoins)
        }.await()
    }

    suspend fun spendCoins(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        val userDoc = firestore.collection("users").document(uid)
        firestore.runTransaction {
            val snapshot = it.get(userDoc)
            var newCoins = (snapshot.getLong("coins") ?: 0) - amount
            if (newCoins < 0) newCoins = 0
            it.update(userDoc, "coins", newCoins)
        }.await()
    }
}