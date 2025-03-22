package com.manujain.notesy.feature_notes.domain.repository

import com.manujain.notesy.feature_notes.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {

    fun getNotes(): Flow<List<Note>>

    suspend fun getNote(noteId: String): Note

    suspend fun insertNote(note: Note)

    suspend fun deleteNote(note: Note)
}
