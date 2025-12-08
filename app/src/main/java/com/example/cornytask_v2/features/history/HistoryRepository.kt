package com.example.cornytask_v2.features.history

import com.example.cornytask_v2.data.History
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class HistoryRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getHistoryFlow(): Flow<List<History>> = callbackFlow {
        var snapshotListener: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            snapshotListener?.remove()

            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(emptyList())
            } else {
                val collection = firestore.collection("users").document(user.uid).collection("history")
                snapshotListener = collection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val history = snapshot?.documents?.mapNotNull {
                            it.toObject(History::class.java)?.copy(id = it.id)
                        } ?: emptyList()

                        trySend(history)
                    }
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            snapshotListener?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }
}
