package com.nobadhabbits.cornytask.features.user

import com.nobadhabbits.cornytask.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
                        // Launch in the flow's scope to perform the async operation
                        this.launch {
                            try {
                                val newUser = User(uid = user.uid, coins = 0, firstLogin = true)
                                userDocRef.set(newUser)
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

    // One-shot fetch for widget
    suspend fun fetchCurrentUser(): User? {
        val user = auth.currentUser ?: return null
        return try {
            firestore.collection("users").document(user.uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun addCoins(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        val userDoc = firestore.collection("users").document(uid)
        userDoc.update("coins", FieldValue.increment(amount.toLong()))
    }

    fun spendCoins(amount: Int) {
        val uid = auth.currentUser?.uid ?: return
        val userDoc = firestore.collection("users").document(uid)
        // Using a transaction to ensure coins don't go negative would not work offline.
        // It is assumed that the UI will prevent the user from spending more coins than they have.
        // For full offline support, this is a necessary trade-off.
        userDoc.update("coins", FieldValue.increment(-amount.toLong()))
    }

    fun updateFcmToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).update("fcmToken", token)
    }

    suspend fun deleteUser() {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid).delete().await()
        user.delete().await()
        auth.signOut()
    }
}
