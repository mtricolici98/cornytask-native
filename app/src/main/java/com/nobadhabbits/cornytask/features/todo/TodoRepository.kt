package com.nobadhabbits.cornytask.features.todo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.nobadhabbits.cornytask.data.History
import com.nobadhabbits.cornytask.data.Todo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class TodoRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getTodosFlow(): Flow<List<Todo>> = callbackFlow {
        var snapshotListener: ListenerRegistration? = null

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            snapshotListener?.remove()
            val user = firebaseAuth.currentUser
            if (user == null) {
                trySend(emptyList())
            } else {
                val collection = firestore.collection("users").document(user.uid).collection("todos")
                snapshotListener = collection
                    .whereEqualTo("isCompleted", false)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val todos = snapshot?.documents?.mapNotNull {
                            it.toObject(Todo::class.java)?.copy(id = it.id)
                        } ?: emptyList()
                        trySend(todos)
                    }
            }
        }

        auth.addAuthStateListener(authListener)

        awaitClose {
            snapshotListener?.remove()
            auth.removeAuthStateListener(authListener)
        }
    }

    suspend fun fetchAllTodos(): List<Todo> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            firestore.collection("users").document(user.uid).collection("todos")
                .whereEqualTo("isCompleted", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
                .documents.mapNotNull {
                    it.toObject(Todo::class.java)?.copy(id = it.id)
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addTodo(title: String, description: String, rewardCoins: Int, dueDate: Date?) {
        val uid = auth.currentUser?.uid ?: return
        val keywords = title.lowercase().split(" ").filter { it.isNotBlank() }
        val todo = Todo(title = title, description = description, rewardCoins = rewardCoins, keywords = keywords, dueDate = dueDate)
        firestore.collection("users").document(uid).collection("todos").add(todo)
    }

    suspend fun upsertCalendarEvent(event: Todo) {
        val uid = auth.currentUser?.uid ?: return
        val collection = firestore.collection("users").document(uid).collection("todos")
        val existing = collection.document(event.id).get().await()
        if (existing.exists()) {
            collection.document(event.id).set(event)
        } else {
            collection.document(event.id).set(event)
        }
    }

    suspend fun getSuggestions(query: String): List<Todo> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return firestore.collection("users").document(uid).collection("todos")
            .whereArrayContains("keywords", query.lowercase())
            .limit(5)
            .get()
            .await()
            .toObjects(Todo::class.java)
    }

    fun updateTodoStatus(todo: Todo, isCompleted: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val historyCollection = firestore.collection("users").document(uid).collection("history")
        val todoDoc = firestore.collection("users").document(uid).collection("todos").document(todo.id)

        firestore.runBatch { batch ->
            if (isCompleted) {
                val newHistoryRef = historyCollection.document()
                val history = History(
                    title = todo.title,
                    rewardCoins = todo.rewardCoins
                )
                batch.set(newHistoryRef, history)
                batch.update(todoDoc, "isCompleted", true, "historyId", newHistoryRef.id)
            } else {
                todo.historyId?.let {
                    val historyDoc = historyCollection.document(it)
                    batch.delete(historyDoc)
                }
                batch.update(todoDoc, "isCompleted", false, "historyId", null)
            }
        }
    }

    fun deleteTodo(todo: Todo) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).collection("todos").document(todo.id).delete()
    }

    suspend fun getTodo(todoId: String): Todo? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("users").document(uid).collection("todos").document(todoId)
            .get()
            .await()
            .toObject(Todo::class.java)?.copy(id = todoId)
    }

    fun updateTodo(todoId: String, title: String, description: String, rewardCoins: Int, dueDate: Date?) {
        val uid = auth.currentUser?.uid ?: return
        val keywords = title.lowercase().split(" ").filter { it.isNotBlank() }
        val todoUpdate = mapOf(
            "title" to title,
            "description" to description,
            "rewardCoins" to rewardCoins,
            "keywords" to keywords,
            "dueDate" to dueDate
        )
        firestore.collection("users").document(uid).collection("todos").document(todoId).update(todoUpdate)
    }
}
