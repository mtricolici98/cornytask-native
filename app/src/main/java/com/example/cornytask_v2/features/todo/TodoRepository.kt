package com.example.cornytask_v2.features.todo

import com.example.cornytask_v2.data.History
import com.example.cornytask_v2.data.Todo
import com.google.firebase.auth.FirebaseAuth
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

    // One-shot fetch for widget
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

    suspend fun addTodo(title: String, description: String, rewardCoins: Int) {
        val uid = auth.currentUser?.uid ?: return
        val keywords = title.lowercase().split(" ").filter { it.isNotBlank() }
        val todo = Todo(title = title, description = description, rewardCoins = rewardCoins, keywords = keywords)
        firestore.collection("users").document(uid).collection("todos").add(todo).await()
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

    suspend fun getTodo(todoId: String): Todo? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection("users").document(uid).collection("todos").document(todoId)
            .get()
            .await()
            .toObject(Todo::class.java)?.copy(id = todoId)
    }

    suspend fun updateTodo(todoId: String, title: String, description: String, rewardCoins: Int) {
        val uid = auth.currentUser?.uid ?: return
        val keywords = title.lowercase().split(" ").filter { it.isNotBlank() }
        val todoUpdate = mapOf(
            "title" to title,
            "description" to description,
            "rewardCoins" to rewardCoins,
            "keywords" to keywords
        )
        firestore.collection("users").document(uid).collection("todos").document(todoId).update(todoUpdate).await()
    }
}