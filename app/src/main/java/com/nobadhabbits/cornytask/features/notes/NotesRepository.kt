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

    /**
     * Writes the note and returns it with its id. New notes get their id up front, so saving
     * the same note again (autosave) updates it instead of creating a duplicate.
     *
     * Not suspending on purpose: Firestore queues the write locally right away, so this is
     * safe to call while a screen is being torn down.
     */
    fun saveNote(note: Note): Note? {
        val collection = notesCollection() ?: return null
        val doc = if (note.id.isBlank()) collection.document() else collection.document(note.id)
        val saved = note.copy(id = doc.id)
        doc.set(saved)
        return saved
    }

    fun deleteNotes(noteIds: Collection<String>) {
        val collection = notesCollection() ?: return
        firestore.runBatch { batch ->
            noteIds.forEach { batch.delete(collection.document(it)) }
        }
    }
}
