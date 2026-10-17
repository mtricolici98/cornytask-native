package com.nobadhabbits.cornytask.features.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nobadhabbits.cornytask.data.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class NotesViewModel : ViewModel() {

    private val repository = NotesRepository()

    val notes = repository.getNotesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveNote(note: Note): Note? = repository.saveNote(note)

    fun deleteNotes(noteIds: Collection<String>) = repository.deleteNotes(noteIds)

    suspend fun getNote(noteId: String): Note? {
        return repository.getNote(noteId)
    }
}
