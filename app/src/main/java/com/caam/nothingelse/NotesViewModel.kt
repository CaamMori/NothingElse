package com.caam.nothingelse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.caam.nothingelse.data.AppDatabase
import com.caam.nothingelse.data.Note
import com.caam.nothingelse.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NoteRepository(
        Room.databaseBuilder(application, AppDatabase::class.java, "nothingelse-db").build().noteDao()
    )

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    private val _archivedNotes = MutableStateFlow<List<Note>>(emptyList())
    val archivedNotes: StateFlow<List<Note>> = _archivedNotes.asStateFlow()

    init {
        refresh()
    }

    fun createNote(onCreated: (Note) -> Unit) {
        viewModelScope.launch {
            val created = withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val draft = Note(id = 0, createdAt = now, updatedAt = now)
                draft.copy(id = repository.insert(draft))
            }
            refresh()
            onCreated(created)
        }
    }

    fun save(note: Note) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.update(note.copy(updatedAt = System.currentTimeMillis()))
            }
            refresh()
        }
    }

    fun delete(note: Note) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(note) }
            refresh()
        }
    }

    fun setPinned(note: Note, pinned: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.update(note.copy(pinned = pinned, updatedAt = System.currentTimeMillis()))
            }
            refresh()
        }
    }

    fun setArchived(note: Note, archived: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.update(note.copy(archived = archived, updatedAt = System.currentTimeMillis()))
            }
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val (latest, archived) = withContext(Dispatchers.IO) {
                repository.getAll() to repository.getArchived()
            }
            _notes.value = latest
            _archivedNotes.value = archived
        }
    }
}
