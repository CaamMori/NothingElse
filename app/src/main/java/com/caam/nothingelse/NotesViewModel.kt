package com.caam.nothingelse

import android.app.Application
import android.content.Context
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
    private val orderPreferences = application.getSharedPreferences("note-order", Context.MODE_PRIVATE)
    private val orderKey = "note_ids"

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

    fun deleteNotes(notes: List<Note>) = viewModelScope.launch {
        if (notes.isEmpty()) return@launch
        dataMutex.withLock {
            withContext(Dispatchers.IO) { repository.removeAll(notes.map(Note::id).distinct()) }
            refreshLocked()
        }
    }

    fun reorder(visibleNotes: List<Note>, fromIndex: Int, toIndex: Int) = viewModelScope.launch {
        if (fromIndex == toIndex || fromIndex !in visibleNotes.indices || toIndex !in visibleNotes.indices) return@launch
        dataMutex.withLock {
            val visibleIds = visibleNotes.map(Note::id)
            val currentSource = listOf(_notes.value, _favoriteNotes.value).singleOrNull { source ->
                val sourceIds = source.mapTo(mutableSetOf(), Note::id)
                visibleIds.all(sourceIds::contains)
            } ?: return@withLock
            val sourcePositions = visibleIds.map { id -> currentSource.indexOfFirst { it.id == id } }
            val allIds = currentIds(_notes.value, _favoriteNotes.value).toMutableList()
            val positions = visibleNotes.map { allIds.indexOf(it.id) }
            if (visibleIds.size != visibleIds.distinct().size ||
                sourcePositions.any { it < 0 } ||
                sourcePositions.zipWithNext().any { (first, second) -> first >= second } ||
                visibleNotes.indices.any { currentSource[sourcePositions[it]].pinned != visibleNotes[it].pinned } ||
                positions.any { it < 0 } ||
                positions.size != positions.distinct().size
            ) return@withLock

            val reorderedVisibleIds = visibleIds.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            positions.forEachIndexed { index, position -> allIds[position] = reorderedVisibleIds[index] }
            orderPreferences.edit().putString(orderKey, allIds.distinct().joinToString(",")).apply()
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
        val ids = reconcileOrder(active, favorites)
        _notes.value = applyOrder(active, ids)
        _favoriteNotes.value = applyOrder(favorites, ids)
    }

    private fun reconcileOrder(active: List<Note>, favorites: List<Note>): List<Long> {
        val all = currentIds(active, favorites)
        val stored = orderPreferences.getString(orderKey, "").orEmpty()
            .split(",").mapNotNull(String::toLongOrNull)
        val reconciled = (stored.filter(all::contains) + all.filterNot(stored::contains)).distinct()
        if (reconciled != stored) {
            orderPreferences.edit().putString(orderKey, reconciled.joinToString(",")).apply()
        }
        return reconciled
    }

    private fun currentIds(active: List<Note>, favorites: List<Note>): List<Long> =
        (active + favorites).map(Note::id).distinct()

    private fun applyOrder(notes: List<Note>, ids: List<Long>): List<Note> {
        val order = ids.withIndex().associate { it.value to it.index }
        return notes.sortedWith(
            compareByDescending<Note> { it.pinned }.thenBy { order[it.id] ?: Int.MAX_VALUE }
        )
    }
}
