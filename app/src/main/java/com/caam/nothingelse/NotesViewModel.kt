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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NoteRepository(
        Room.databaseBuilder(application, AppDatabase::class.java, "nothingelse-db").build().noteDao()
    )

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes = _notes.asStateFlow()
    private val _favoriteNotes = MutableStateFlow<List<Note>>(emptyList())
    val favoriteNotes = _favoriteNotes.asStateFlow()
    private val dataMutex = Mutex()

    init { refresh() }

    fun create(onCreated: (Note) -> Unit) = viewModelScope.launch {
        val note = dataMutex.withLock {
            val created = withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val draft = Note(id = 0, createdAt = now, updatedAt = now)
                draft.copy(id = repository.create(draft))
            }
            refreshLocked()
            created
        }
        onCreated(note)
    }

    fun save(note: Note, onSaved: (() -> Unit)? = null) = viewModelScope.launch {
        dataMutex.withLock {
            withContext(Dispatchers.IO) {
                repository.save(note.copy(updatedAt = System.currentTimeMillis()))
            }
            refreshLocked()
        }
        onSaved?.invoke()
    }

    fun delete(note: Note) = viewModelScope.launch {
        dataMutex.withLock {
            withContext(Dispatchers.IO) { repository.remove(note) }
            refreshLocked()
        }
    }

    fun setPinned(note: Note, pinned: Boolean) = save(note.copy(pinned = pinned))
    fun setFavorite(note: Note, favorite: Boolean) = save(note.copy(archived = favorite))

    fun refresh() = viewModelScope.launch {
        dataMutex.withLock { refreshLocked() }
    }

    private suspend fun refreshLocked() {
        val (active, favorites) = withContext(Dispatchers.IO) {
            repository.activeNotes() to repository.favoriteNotes()
        }
        _notes.value = active
        _favoriteNotes.value = favorites
    }
}
