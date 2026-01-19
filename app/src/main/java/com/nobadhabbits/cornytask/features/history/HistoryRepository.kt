package com.nobadhabbits.cornytask.features.history

import com.nobadhabbits.cornytask.data.History
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class HistoryRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val historyCollection
        get() = auth.currentUser?.uid?.let {
            firestore.collection("users").document(it).collection("history")
        }

    fun getHistoryFlow(): Flow<List<History>> = callbackFlow {
        var snapshotListener: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            snapshotListener?.remove()

            if (firebaseAuth.currentUser == null) {
                trySend(emptyList())
            } else {
                snapshotListener = historyCollection
                    ?.orderBy("createdAt", Query.Direction.DESCENDING)
                    ?.addSnapshotListener { snapshot, error ->
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

    fun newHistoryRef(): DocumentReference? =
        historyCollection?.document() // creates ID locally (offline-safe)

    fun addHistoryEntry(history: History) {
        historyCollection?.add(history)
    }
}
