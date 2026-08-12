package com.caam.nothingelse

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.caam.nothingelse.data.AppDatabase
import com.caam.nothingelse.data.Note
import com.caam.nothingelse.data.NoteCreationService
import com.caam.nothingelse.data.NoteRepository
import com.caam.nothingelse.ui.DEFAULT_NOTEBOOK_NAME
import com.caam.nothingelse.ui.deletedAtFromNoteBody
import com.caam.nothingelse.ui.isDeletedNoteBody
import com.caam.nothingelse.ui.isDeletedNoteExpired
import com.caam.nothingelse.ui.markNoteBodyDeleted
import com.caam.nothingelse.ui.notebookFromNoteBody
import com.caam.nothingelse.ui.renameNoteBodyNotebook
import com.caam.nothingelse.ui.restoreDeletedNoteBody
import com.caam.nothingelse.ui.setNoteBodyNotebook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NoteCreationState(val isCreating: Boolean = false)

internal fun mergeVisibleNoteOrder(currentIds: List<Long>, visibleIds: List<Long>): List<Long> {
    if (visibleIds.isEmpty() || visibleIds.size != visibleIds.distinct().size || !currentIds.containsAll(visibleIds)) {
        return currentIds
    }
    val visiblePositions = currentIds.indices.filter { currentIds[it] in visibleIds }
    if (visiblePositions.size != visibleIds.size) return currentIds
    return currentIds.toMutableList().apply {
        visiblePositions.forEachIndexed { index, position -> this[position] = visibleIds[index] }
    }
}

/**
 * 排序行为表
 * | 模式 \ 操作 | 新建笔记 | 编辑笔记 |
 * |---|---|---|
 * | 默认模式 (isUserOrdered=false) | 按 updatedAt DESC 自动置顶 | 自动重排至顶部（按最新 updatedAt） |
 * | 手动模式 (isUserOrdered=true) | 插入列表最上方（视为隐式最新编辑，不追加末尾） | 不重排，保持手动顺序不变 |
 *
 * 已知缺口：不提供"恢复默认排序"入口（无 UI 将 isUserOrdered 重置为 false）。
 */
class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NoteRepository(
        Room.databaseBuilder(application, AppDatabase::class.java, "nothingelse-db").build().noteDao()
    )

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes = _notes.asStateFlow()
    private val _favoriteNotes = MutableStateFlow<List<Note>>(emptyList())
    val favoriteNotes = _favoriteNotes.asStateFlow()
    private val _deletedNotes = MutableStateFlow<List<Note>>(emptyList())
    val deletedNotes = _deletedNotes.asStateFlow()
    private val _notebooks = MutableStateFlow(listOf(DEFAULT_NOTEBOOK_NAME))
    val notebooks = _notebooks.asStateFlow()
    private val creationService = NoteCreationService()
    private val _creationState = MutableStateFlow(NoteCreationState())
    val creationState = _creationState.asStateFlow()
    private val dataMutex = Mutex()
    private val orderPreferences = application.getSharedPreferences("note-order", Context.MODE_PRIVATE)
    private val orderKey = "note_ids"
    private val userOrderedKey = "user_ordered"
    private val notebookPreferences = application.getSharedPreferences("notebooks", Context.MODE_PRIVATE)
    private val customNotebooksKey = "custom_notebooks"

    init { refresh() }

    fun createBlank(notebook: String, onComplete: (Result<Note>) -> Unit) =
        createNote(notebook, "", "", onComplete)

    fun createNotebook(name: String): Result<String> = runCatching {
        val notebook = name.trim()
        require(notebook.isNotBlank()) { "Notebook name cannot be blank" }
        val existing = _notebooks.value.map(String::trim)
        require(existing.none { it.equals(notebook, ignoreCase = true) }) { "Notebook already exists" }
        val customNotebooks = loadCustomNotebooks() + notebook
        persistCustomNotebooks(customNotebooks)
        _notebooks.value = mergeNotebooks(_notebooks.value + notebook)
        notebook
    }

    fun createFromImport(
        notebook: String,
        rawContent: String,
        onComplete: (Result<Note>) -> Unit
    ) {
        val prepared = runCatching { creationService.prepareImport(rawContent) }
            .getOrElse { error ->
                onComplete(Result.failure(error))
                return
            }
        createNote(notebook, prepared.title, prepared.content, onComplete)
    }

    private fun createNote(
        notebook: String,
        title: String,
        body: String,
        onComplete: (Result<Note>) -> Unit
    ) {
        if (_creationState.value.isCreating) return
        _creationState.value = NoteCreationState(isCreating = true)
        viewModelScope.launch {
            val result = runCatching {
                dataMutex.withLock {
                    val created = withContext(Dispatchers.IO) {
                        val now = System.currentTimeMillis()
                        val draft = Note(
                            id = 0,
                            title = title,
                            body = setNoteBodyNotebook(body, notebook),
                            createdAt = now,
                            updatedAt = now
                        )
                        draft.copy(id = repository.create(draft))
                    }
                    if (orderPreferences.getBoolean(userOrderedKey, false)) {
                        val stored = orderPreferences.getString(orderKey, "").orEmpty()
                            .split(",").mapNotNull(String::toLongOrNull)
                        val updated = listOf(created.id) + stored.filterNot { it == created.id }
                        orderPreferences.edit().putString(orderKey, updated.joinToString(",")).apply()
                    }
                    refreshLocked()
                    created
                }
            }
            _creationState.value = NoteCreationState()
            onComplete(result)
        }
    }

    fun save(note: Note, onSaved: (() -> Unit)? = null) = viewModelScope.launch {
        dataMutex.withLock {
            withContext(Dispatchers.IO) {
                val persisted = repository.note(note.id)
                if (persisted == null || !isDeletedNoteBody(persisted.body)) {
                    repository.save(note.copy(updatedAt = System.currentTimeMillis()))
                }
            }
            refreshLocked()
        }
        onSaved?.invoke()
    }

    fun delete(note: Note) = viewModelScope.launch {
        dataMutex.withLock {
            val deletedAt = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                repository.save(note.copy(body = markNoteBodyDeleted(note.body, deletedAt), updatedAt = deletedAt))
            }
            refreshLocked()
        }
    }

    fun deleteNotes(notes: List<Note>) = viewModelScope.launch {
        if (notes.isEmpty()) return@launch
        dataMutex.withLock {
            val deletedAt = System.currentTimeMillis()
            withContext(Dispatchers.IO) {
                notes.distinctBy(Note::id).forEach { note ->
                    repository.save(note.copy(body = markNoteBodyDeleted(note.body, deletedAt), updatedAt = deletedAt))
                }
            }
            refreshLocked()
        }
    }

    fun restoreNote(note: Note) = viewModelScope.launch {
        restoreNotesLocked(listOf(note))
    }

    fun restoreNotes(notes: List<Note>) = viewModelScope.launch {
        if (notes.isEmpty()) return@launch
        restoreNotesLocked(notes)
    }

    private suspend fun restoreNotesLocked(notes: List<Note>) {
        dataMutex.withLock {
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                notes.distinctBy(Note::id).forEach { note ->
                    repository.note(note.id)
                        ?.takeIf { isDeletedNoteBody(it.body) }
                        ?.let { repository.save(it.copy(body = restoreDeletedNoteBody(it.body), updatedAt = now)) }
                }
            }
            refreshLocked()
        }
    }

    fun permanentlyDelete(note: Note) = viewModelScope.launch {
        dataMutex.withLock {
            withContext(Dispatchers.IO) {
                repository.note(note.id)?.takeIf { isDeletedNoteBody(it.body) }?.let(repository::remove)
            }
            refreshLocked()
        }
    }

    fun permanentlyDeleteNotes(notes: List<Note>) = viewModelScope.launch {
        val requestedIds = notes.map(Note::id).distinct()
        if (requestedIds.isEmpty()) return@launch
        dataMutex.withLock {
            withContext(Dispatchers.IO) {
                val deletedIds = requestedIds.filter { id ->
                    repository.note(id)?.let { isDeletedNoteBody(it.body) } == true
                }
                if (deletedIds.isNotEmpty()) repository.removeAll(deletedIds)
            }
            refreshLocked()
        }
    }

    fun reorder(visibleNotes: List<Note>) = viewModelScope.launch {
        val visibleIds = visibleNotes.map(Note::id)
        if (visibleIds.isEmpty() || visibleIds.size != visibleIds.distinct().size) return@launch
        dataMutex.withLock {
            val currentSource = listOf(_notes.value, _favoriteNotes.value).singleOrNull { source ->
                val sourceIds = source.mapTo(mutableSetOf(), Note::id)
                visibleIds.all(sourceIds::contains)
            } ?: return@withLock
            val notesById = currentSource.associateBy(Note::id)
            val finalVisibleNotes = visibleIds.map { id -> notesById[id] ?: return@withLock }
            if (finalVisibleNotes.zipWithNext().any { (first, second) -> !first.pinned && second.pinned }) {
                return@withLock
            }
            val allIds = currentIds(_notes.value, _favoriteNotes.value)
            val reorderedIds = mergeVisibleNoteOrder(allIds, visibleIds)
            if (reorderedIds == allIds) return@withLock
            orderPreferences.edit()
                .putString(orderKey, reorderedIds.joinToString(","))
                .putBoolean(userOrderedKey, true)
                .apply()
            refreshLocked()
        }
    }

    fun setPinned(note: Note, pinned: Boolean) = save(note.copy(pinned = pinned))
    fun setFavorite(note: Note, favorite: Boolean) = save(note.copy(archived = favorite))
    fun setNotebook(note: Note, notebook: String) = save(note.copy(body = setNoteBodyNotebook(note.body, notebook)))

    fun renameNotebook(oldName: String, newName: String) = viewModelScope.launch {
        val targetName = newName.trim()
        if (oldName == DEFAULT_NOTEBOOK_NAME || targetName.isBlank()) return@launch
        dataMutex.withLock {
            withContext(Dispatchers.IO) {
                (repository.activeNotes() + repository.favoriteNotes())
                    .distinctBy(Note::id)
                    .forEach { note ->
                        val renamedBody = renameNoteBodyNotebook(note.body, oldName, targetName)
                        if (renamedBody != note.body) {
                            repository.save(note.copy(body = renamedBody, updatedAt = System.currentTimeMillis()))
                        }
                    }
            }
            val renamedCustomNotebooks = loadCustomNotebooks().map { notebook ->
                if (notebook == oldName) targetName else notebook
            }
            persistCustomNotebooks(renamedCustomNotebooks)
            refreshLocked()
        }
    }

    fun deleteNotebook(name: String) = viewModelScope.launch {
        val notebook = name.trim()
        if (notebook == DEFAULT_NOTEBOOK_NAME || notebook.isBlank()) return@launch
        dataMutex.withLock {
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                repository.allNotes()
                    .filter { notebookFromNoteBody(it.body) == notebook }
                    .forEach { note ->
                        repository.save(
                            note.copy(
                                body = setNoteBodyNotebook(note.body, DEFAULT_NOTEBOOK_NAME),
                                updatedAt = now
                            )
                        )
                    }
            }
            persistCustomNotebooks(loadCustomNotebooks().filterNot { it == notebook })
            refreshLocked()
        }
    }

    fun refresh() = viewModelScope.launch {
        dataMutex.withLock { refreshLocked() }
    }

    private suspend fun refreshLocked() {
        val (activeRows, favoriteRows) = withContext(Dispatchers.IO) {
            val loadedActive = repository.activeNotes()
            val loadedFavorites = repository.favoriteNotes()
            val expiredIds = (loadedActive + loadedFavorites)
                .filter { isDeletedNoteExpired(it.body, System.currentTimeMillis()) }
                .map(Note::id)
                .distinct()
            if (expiredIds.isNotEmpty()) repository.removeAll(expiredIds)
            loadedActive.filterNot { it.id in expiredIds } to loadedFavorites.filterNot { it.id in expiredIds }
        }
        val allRows = activeRows + favoriteRows
        val active = activeRows.filterNot { isDeletedNoteBody(it.body) }
        val favorites = favoriteRows.filterNot { isDeletedNoteBody(it.body) }
        val deleted = allRows.filter { isDeletedNoteBody(it.body) }
            .sortedByDescending { deletedAtFromNoteBody(it.body) ?: it.updatedAt }
        val isUserOrdered = orderPreferences.getBoolean(userOrderedKey, false)
        if (isUserOrdered) {
            val ids = reconcileOrder(active, favorites)
            _notes.value = applyOrder(active, ids)
            _favoriteNotes.value = applyOrder(favorites, ids)
        } else {
            _notes.value = active.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
            _favoriteNotes.value = favorites.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
        }
        _deletedNotes.value = deleted
        _notebooks.value = mergeNotebooks(loadCustomNotebooks() + allRows.map { notebookFromNoteBody(it.body) })
    }

    private fun loadCustomNotebooks(): List<String> = notebookPreferences
        .getStringSet(customNotebooksKey, emptySet())
        .orEmpty()
        .map(String::trim)
        .filter { it.isNotBlank() && it != DEFAULT_NOTEBOOK_NAME }
        .distinct()

    private fun persistCustomNotebooks(notebooks: List<String>) {
        val cleaned = notebooks
            .map(String::trim)
            .filter { it.isNotBlank() && it != DEFAULT_NOTEBOOK_NAME }
            .distinct()
        notebookPreferences.edit().putStringSet(customNotebooksKey, cleaned.toSet()).apply()
    }

    private fun mergeNotebooks(notebooks: List<String>): List<String> =
        (listOf(DEFAULT_NOTEBOOK_NAME) + notebooks)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()

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
