package com.nobadhabbits.cornytask.features.notes

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nobadhabbits.cornytask.data.Note
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotesRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun notesCollection() = auth.currentUser?.uid?.let {
        firestore.collection("users").document(it).collection("notes")
    }

    fun getNotesFlow(): Flow<List<Note>> = callbackFlow {
        val listener = notesCollection()
            ?.orderBy("timestamp", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notes = snapshot.documents.map {
                        it.toObject(Note::class.java)!!.copy(id = it.id)
                    }
                    trySend(notes)
                }
            }
        awaitClose { listener?.remove() }
    }

    suspend fun getNote(id: String): Note? {
        val doc = notesCollection()?.document(id)?.get()?.await()
        return doc?.toObject(Note::class.java)?.copy(id = doc.id)
    }

    suspend fun saveNote(note: Note) {
        val collection = notesCollection() ?: return
        if (note.id.isBlank()) {
            collection.add(note)
        } else {
            collection.document(note.id).set(note)
        }
    }

    suspend fun deleteNote(noteId: String) {
        notesCollection()?.document(noteId)?.delete()
    }
}
