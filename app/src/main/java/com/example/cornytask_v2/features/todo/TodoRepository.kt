package com.example.cornytask_v2.features.todo

import com.example.cornytask_v2.data.History
import com.example.cornytask_v2.data.Todo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TodoRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getTodosFlow(): Flow<List<Todo>> = callbackFlow {
        var snapshotListener: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            // Whenever auth state changes, remove the old listener to prevent leaks
            snapshotListener?.remove()

            val user = firebaseAuth.currentUser
            if (user == null) {
                // If user logs out, send an empty list and we'''re done.
                trySend(emptyList())
            } else {
                // If user logs in, create a new Firestore listener for their data.
                val collection = firestore.collection("users").document(user.uid).collection("todos")
                snapshotListener = collection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error) // Close the flow on error
                            return@addSnapshotListener
                        }
                        // Map the documents, making sure to include the ID
                        val todos = snapshot?.documents?.mapNotNull {
                            it.toObject(Todo::class.java)?.copy(id = it.id)
                        } ?: emptyList()

                        trySend(todos) // Send the latest list of todos
                    }
            }
        }

        // Start listening for authentication changes.
        // This will also trigger immediately with the current auth state.
        auth.addAuthStateListener(authListener)

        // When the flow is cancelled (e.g., ViewModel is cleared),
        // remove both listeners to prevent memory leaks.
        awaitClose {
            snapshotListener?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }


    suspend fun updateTodoStatus(todo: Todo, isCompleted: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val historyCollection = firestore.collection("users").document(uid).collection("history")
        val todoDoc = firestore.collection("users").document(uid).collection("todos").document(todo.id)

        if (isCompleted) {
            val history = History(title = todo.title, rewardCoins = todo.rewardCoins)
            val historyRef = historyCollection.add(history).await()
            todoDoc.update("isCompleted", true, "historyId", historyRef.id).await()
        } else {
            todo.historyId?.let { historyCollection.document(it).delete().await() }
            todoDoc.update("isCompleted", false, "historyId", null).await()
        }
    }

    suspend fun deleteTodo(todo: Todo) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("todos").document(todo.id).delete().await()
    }

    suspend fun resetTodo(todo: Todo) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("todos").document(todo.id).update(
            "isCompleted", false,
            "createdAt", FieldValue.serverTimestamp()
        ).await()
    }
}