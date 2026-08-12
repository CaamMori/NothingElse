package com.caam.nothingelse.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.caam.nothingelse.data.Note
import com.caam.nothingelse.data.BlankImportException
import com.caam.nothingelse.R
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class NoteFilter { Notes, Favorites, RecentlyDeleted }
private enum class CreationTextMode { Import }

@Composable
fun NotesHomeScreen(
    notes: List<Note>, favoriteNotes: List<Note>, deletedNotes: List<Note>, notebooks: List<String>, onOpenNote: (Note) -> Unit,
    isCreatingNote: Boolean,
    onCreateBlank: (String, (Result<Note>) -> Unit) -> Unit,
    onCreateNotebook: (String) -> Result<String>,
    onCreateFromImport: (String, String, (Result<Note>) -> Unit) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onDeleteNotes: (List<Note>) -> Unit,
    onRestoreNote: (Note) -> Unit,
    onRestoreNotes: (List<Note>) -> Unit,
    onPermanentlyDeleteNote: (Note) -> Unit,
    onPermanentlyDeleteNotes: (List<Note>) -> Unit,
    onReorderNotes: (List<Note>) -> Unit,
    onSetNotebook: (Note, String) -> Unit,
    onRenameNotebook: (String, String) -> Unit,
    onDeleteNotebook: (String) -> Unit,
    onSetPinned: (Note, Boolean) -> Unit, onSetFavorite: (Note, Boolean) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(NoteFilter.Notes) }
    var actionsFor by remember { mutableStateOf<Note?>(null) }
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var selectedNotebook by remember { mutableStateOf<String?>(null) }
    var creatingNotebook by remember { mutableStateOf(false) }
    var renamingNotebook by remember { mutableStateOf<String?>(null) }
    var deletingNotebook by remember { mutableStateOf<String?>(null) }
    var movingSelection by remember { mutableStateOf(false) }
    var showingCreationSheet by remember { mutableStateOf(false) }
    var creationTextMode by remember { mutableStateOf<CreationTextMode?>(null) }
    var creationError by remember { mutableStateOf<Int?>(null) }
    var pendingCreatedNote by remember { mutableStateOf<Note?>(null) }
    var recentlyDeletedForUndo by remember { mutableStateOf(emptyList<Note>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val filteredNotes = notes.filterByNotebook(selectedNotebook)
    val filteredFavorites = favoriteNotes.filterByNotebook(selectedNotebook)
    val source = when (filter) {
        NoteFilter.Notes -> filteredNotes
        NoteFilter.Favorites -> filteredFavorites
        NoteFilter.RecentlyDeleted -> deletedNotes
    }
    val visible = source.filter { matchesSearch(it, query) }
    val selectionMode = selectedIds.isNotEmpty()
    fun selectedNotebookName() = selectedNotebook ?: DEFAULT_NOTEBOOK_NAME
    fun handleCreationResult(result: Result<Note>, closeOverlay: () -> Unit) {
        result.fold(
            onSuccess = { note ->
                creationError = null
                closeOverlay()
                pendingCreatedNote = note
            },
            onFailure = { error ->
                creationError = if (error is BlankImportException) R.string.error_empty_import
                else R.string.error_note_creation
                if (error !is BlankImportException) {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_note_creation)) }
                }
            }
        )
    }
    LaunchedEffect(pendingCreatedNote?.id) {
        val note = pendingCreatedNote ?: return@LaunchedEffect
        delay(250)
        onOpenNote(note)
        if (pendingCreatedNote?.id == note.id) pendingCreatedNote = null
    }
    LaunchedEffect(visible.map(Note::id)) {
        selectedIds = selectedIds.intersect(visible.mapTo(mutableSetOf(), Note::id))
        if (selectedIds.isEmpty()) confirmingDelete = false
    }
    LaunchedEffect(recentlyDeletedForUndo) {
        val deleted = recentlyDeletedForUndo
        if (deleted.isEmpty()) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = if (deleted.size == 1) "Note moved to Recently Deleted" else "${deleted.size} notes moved to Recently Deleted",
            actionLabel = "Undo",
            withDismissAction = true
        )
        if (result == SnackbarResult.ActionPerformed) onRestoreNotes(deleted)
        if (recentlyDeletedForUndo.map(Note::id) == deleted.map(Note::id)) recentlyDeletedForUndo = emptyList()
    }
    BackHandler(enabled = movingSelection || confirmingDelete || selectionMode || actionsFor != null || showingCreationSheet || creationTextMode != null) {
        when {
            creationTextMode != null && !isCreatingNote -> creationTextMode = null
            showingCreationSheet -> showingCreationSheet = false
            movingSelection -> movingSelection = false
            actionsFor != null -> actionsFor = null
            confirmingDelete -> confirmingDelete = false
            selectionMode -> selectedIds = emptySet()
        }
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            if (selectionMode) {
                SelectionHeader(
                    selectedCount = selectedIds.size,
                    allSelected = visible.isNotEmpty() && selectedIds == visible.mapTo(mutableSetOf(), Note::id),
                    recentlyDeleted = filter == NoteFilter.RecentlyDeleted,
                    confirmingDelete = confirmingDelete,
                    onExit = { selectedIds = emptySet(); confirmingDelete = false },
                    onSelectAll = {
                        val visibleIds = visible.mapTo(mutableSetOf(), Note::id)
                        selectedIds = if (selectedIds == visibleIds) emptySet()
                            else visibleIds
                    },
                    onRequestMove = { movingSelection = true },
                    onRequestRestore = {
                        val restoring = visible.filter { it.id in selectedIds }
                        onRestoreNotes(restoring)
                        selectedIds = emptySet()
                        confirmingDelete = false
                    },
                    onRequestDelete = { confirmingDelete = true },
                    onCancelDelete = { confirmingDelete = false },
                    onConfirmDelete = {
                        val deleting = visible.filter { it.id in selectedIds }
                        if (filter == NoteFilter.RecentlyDeleted) {
                            onPermanentlyDeleteNotes(deleting)
                        } else {
                            recentlyDeletedForUndo = deleting
                            onDeleteNotes(deleting)
                        }
                        selectedIds = emptySet()
                        confirmingDelete = false
                    }
                )
            } else {
                HomeHeader(
                    filter = filter,
                    noteCount = source.size,
                    onFilterChange = { newFilter ->
                        if (newFilter == NoteFilter.RecentlyDeleted) selectedNotebook = null
                        filter = newFilter
                    },
                    onCreateNote = { showingCreationSheet = true }
                )
                if (filter != NoteFilter.RecentlyDeleted) {
                    NotebookControl(
                        notebooks = notebooks,
                        selectedNotebook = selectedNotebook,
                        onSelect = { selectedNotebook = it },
                        onCreateNotebook = { creatingNotebook = true },
                        onRenameNotebook = { renamingNotebook = it },
                        onDeleteNotebook = { deletingNotebook = it },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
                    )
                }
                QuietSearchField(query, { query = it })
            }
            AnimatedContent(
                targetState = filter,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    fadeIn(animationSpec = tween(180), initialAlpha = 0.45f) togetherWith
                        fadeOut(animationSpec = tween(100))
                },
                label = "notes-content"
            ) { selectedFilter ->
                val selectedNotes = when (selectedFilter) {
                    NoteFilter.Notes -> filteredNotes
                    NoteFilter.Favorites -> filteredFavorites
                    NoteFilter.RecentlyDeleted -> deletedNotes
                }
                val selectedVisible = selectedNotes.filter { matchesSearch(it, query) }
                if (selectedVisible.isEmpty()) {
                    EmptyState(query.isNotBlank(), selectedFilter, Modifier.fillMaxSize())
                } else if (selectedFilter == NoteFilter.RecentlyDeleted) {
                    DeletedNotesList(
                        notes = selectedVisible,
                        query = query,
                        selectedIds = selectedIds,
                        selectionMode = selectionMode,
                        onOpenActions = { actionsFor = it },
                        onLongPress = { selectedIds = selectedIds + it.id },
                        onToggleSelection = { note ->
                            selectedIds = if (note.id in selectedIds) selectedIds - note.id else selectedIds + note.id
                        }
                    )
                } else {
                    ReorderableNotesList(
                        notes = selectedVisible,
                        query = query,
                        selectedIds = selectedIds,
                        selectionMode = selectionMode,
                        onOpen = onOpenNote,
                        onLongPress = { selectedIds = selectedIds + it.id },
                        onToggleSelection = { note ->
                            selectedIds = if (note.id in selectedIds) selectedIds - note.id else selectedIds + note.id
                        },
                        onReorder = onReorderNotes
                    )
                }
            }
        }
        AnimatedContent(
            targetState = actionsFor,
            transitionSpec = {
                    (fadeIn(animationSpec = tween(180), initialAlpha = 0.45f) +
                        slideInVertically(tween(220)) { it / 3 }) togetherWith
                        (fadeOut(animationSpec = tween(120)) +
                            slideOutVertically(tween(180)) { it / 3 })
            },
            label = "note-actions"
        ) { note ->
            if (note != null) {
                if (filter == NoteFilter.RecentlyDeleted) {
                    DeletedActionSheet(
                        note = note,
                        onDismiss = { actionsFor = null },
                        onRestore = { onRestoreNote(note); actionsFor = null },
                        onPermanentlyDelete = { onPermanentlyDeleteNote(note); actionsFor = null }
                    )
                } else {
                    ActionSheet(
                        note = note,
                        onDismiss = { actionsFor = null },
                        onPin = { onSetPinned(note, !note.pinned); actionsFor = null },
                        onFavorite = { onSetFavorite(note, !note.archived); actionsFor = null },
                        notebooks = notebooks,
                        onMoveToNotebook = { notebook -> onSetNotebook(note, notebook); actionsFor = null },
                        onDelete = {
                            recentlyDeletedForUndo = listOf(note)
                            onDeleteNote(note)
                            actionsFor = null
                        }
                    )
                }
            }
        }
        if (creatingNotebook) {
            NotebookNameDialog(
                title = "New notebook",
                confirmLabel = "Create",
                initialName = "",
                forbiddenNames = notebooks.map(String::trim).toSet(),
                onDismiss = { creatingNotebook = false },
                onConfirm = { name ->
                    onCreateNotebook(name).fold(
                        onSuccess = { notebook ->
                            creatingNotebook = false
                            selectedNotebook = notebook
                        },
                        onFailure = {
                            scope.launch { snackbarHostState.showSnackbar("Notebook already exists") }
                        }
                    )
                }
            )
        }
        renamingNotebook?.let { notebook ->
            NotebookNameDialog(
                title = "Rename notebook",
                confirmLabel = "Rename",
                initialName = notebook,
                forbiddenNames = notebooks.filterNot { it == notebook }.map(String::trim).toSet(),
                onDismiss = { renamingNotebook = null },
                onConfirm = { name ->
                    onRenameNotebook(notebook, name)
                    selectedNotebook = name
                    renamingNotebook = null
                }
            )
        }
        deletingNotebook?.let { notebook ->
            AlertDialog(
                onDismissRequest = { deletingNotebook = null },
                title = { Text("Delete notebook?") },
                text = { Text("Notes will move to $DEFAULT_NOTEBOOK_NAME.") },
                confirmButton = {
                    TextButton("Delete", {
                        selectedNotebook = DEFAULT_NOTEBOOK_NAME
                        onDeleteNotebook(notebook)
                        deletingNotebook = null
                    }, MaterialTheme.colorScheme.error)
                },
                dismissButton = { TextButton("Cancel", { deletingNotebook = null }) }
            )
        }
        if (movingSelection) {
            NotebookMoveDialog(
                notebooks = notebooks,
                onDismiss = { movingSelection = false },
                onMove = { notebook ->
                    visible.filter { it.id in selectedIds }.forEach { onSetNotebook(it, notebook) }
                    selectedIds = emptySet()
                    movingSelection = false
                }
            )
        }
        AnimatedVisibility(
            visible = showingCreationSheet,
            enter = fadeIn(tween(150)) + slideInVertically(tween(220)) { it / 3 },
            exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { it / 3 }
        ) {
            NoteCreationSheet(
                onDismiss = { showingCreationSheet = false },
                onCreateNote = {
                    creationError = null
                    onCreateBlank(selectedNotebookName()) { result ->
                        handleCreationResult(result) { showingCreationSheet = false }
                    }
                },
                onImport = {
                    showingCreationSheet = false
                    creationError = null
                    creationTextMode = CreationTextMode.Import
                },
                enabled = !isCreatingNote
            )
        }
        creationTextMode?.let { mode ->
            TextImportDialog(
                title = stringResource(R.string.import_title),
                placeholder = stringResource(R.string.import_placeholder),
                confirmLabel = stringResource(R.string.import_action),
                isCreating = isCreatingNote,
                errorMessage = creationError?.let { stringResource(it) },
                onDismiss = { if (!isCreatingNote) creationTextMode = null },
                onConfirm = { text ->
                    creationError = null
                    onCreateFromImport(selectedNotebookName(), text) { result ->
                        handleCreationResult(result) { creationTextMode = null }
                    }
                }
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp)
        )
    }
}

private fun List<Note>.filterByNotebook(notebook: String?): List<Note> =
    notebook?.let { name -> filter { notebookFromNoteBody(it.body) == name } } ?: this

@Composable
private fun SelectionHeader(
    selectedCount: Int,
    allSelected: Boolean,
    recentlyDeleted: Boolean,
    confirmingDelete: Boolean,
    onExit: () -> Unit,
    onSelectAll: () -> Unit,
    onRequestMove: () -> Unit,
    onRequestRestore: () -> Unit,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(82.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuietIconButton(Icons.Default.Close, "Exit selection", onClick = onExit)
        if (confirmingDelete) {
            Text(
                if (recentlyDeleted) "Delete forever $selectedCount ${if (selectedCount == 1) "note" else "notes"}?"
                else "Delete $selectedCount ${if (selectedCount == 1) "note" else "notes"}?",
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium
            )
            TextButton("Cancel", onCancelDelete)
            TextButton("Delete", onConfirmDelete, MaterialTheme.colorScheme.error)
        } else {
            Text("$selectedCount selected", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(if (allSelected) "Clear" else "Select all", onSelectAll)
            if (recentlyDeleted) {
                QuietIconButton(Icons.Default.Restore, "Restore selected", tint = MaterialTheme.colorScheme.primary, onClick = onRequestRestore)
            } else {
                QuietIconButton(Icons.Default.Folder, "Move selected", tint = MaterialTheme.colorScheme.primary, onClick = onRequestMove)
            }
            QuietIconButton(Icons.Default.DeleteOutline, if (recentlyDeleted) "Permanently delete selected" else "Delete selected", tint = MaterialTheme.colorScheme.error, onClick = onRequestDelete)
        }
    }
}

@Composable
private fun Modifier.quietClickable(
    enabled: Boolean = true,
    pressedAlpha: Float = .62f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled && pressed) pressedAlpha else 1f,
        animationSpec = tween(120),
        label = "quiet-clickable-alpha"
    )
    return alpha(animatedAlpha)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
private fun TextButton(
    label: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Text(
        label,
        modifier.clip(RoundedCornerShape(10.dp)).quietClickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = color
    )
}

@Composable
private fun QuietIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier.size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .quietClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = tint)
    }
}

@Composable
private fun HomeHeader(filter: NoteFilter, noteCount: Int, onFilterChange: (NoteFilter) -> Unit, onCreateNote: () -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 16.dp, top = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "NothingElse",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append("$noteCount ${if (noteCount == 1) "note" else "notes"}")
                        when (filter) {
                            NoteFilter.Notes -> Unit
                            NoteFilter.Favorites -> append(" in Favorites")
                            NoteFilter.RecentlyDeleted -> append(" in Recently Deleted")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (filter == NoteFilter.RecentlyDeleted) {
                    Text(
                        "Notes are permanently deleted after $DELETED_NOTE_RETENTION_DAYS days.",
                        Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (filter != NoteFilter.RecentlyDeleted) {
                QuietIconButton(
                    Icons.Default.Add,
                    "Create note",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onCreateNote
                )
            }
        }
        FilterControl(filter, onFilterChange, Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun FilterControl(selected: NoteFilter, onSelect: (NoteFilter) -> Unit, modifier: Modifier = Modifier) {
    // NOTE: 三个状态 Tab 标签统一为短文本，避免窄屏/大字体下截断。
    // 之前尝试 TextMeasurer + BoxWithConstraints 自适应字号缩放，因 Compose scope
    // receiver 类型问题（toPx/toSp 解析失败）多次编译不过，果断弃用改短标签方案。
    val labels = mapOf(
        "Notes" to NoteFilter.Notes,
        "Favorites" to NoteFilter.Favorites,
        "Deleted" to NoteFilter.RecentlyDeleted
    )

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        labels.forEach { (label, filter) ->
            FilterOption(
                label = label,
                selected = selected == filter,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(filter) }
            )
        }
    }
}

@Composable
private fun FilterOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (selected) {
            Spacer(Modifier.height(5.dp))
            Box(
                Modifier.fillMaxWidth().height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun NotebookControl(
    notebooks: List<String>,
    selectedNotebook: String?,
    onSelect: (String?) -> Unit,
    onCreateNotebook: () -> Unit,
    onRenameNotebook: (String) -> Unit,
    onDeleteNotebook: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        NotebookOption("All", selectedNotebook == null) { onSelect(null) }
        notebooks.distinct().forEach { notebook ->
            NotebookOption(notebook, selectedNotebook == notebook) { onSelect(notebook) }
        }
        NotebookIconOption("New notebook", Icons.Default.Add, onCreateNotebook)
        if (selectedNotebook != null && selectedNotebook != DEFAULT_NOTEBOOK_NAME) {
            NotebookIconOption("Rename notebook", Icons.Default.Edit) { onRenameNotebook(selectedNotebook) }
            NotebookIconOption("Delete notebook", Icons.Default.DeleteOutline) { onDeleteNotebook(selectedNotebook) }
        }
    }
}

private val notebookColors = listOf(
    Color(0xFF1769AA), Color(0xFFB56A2D), Color(0xFF4E7D59),
    Color(0xFF75608E), Color(0xFFB64A4A), Color(0xFF947516)
)

@Composable
private fun NotebookOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val colorIndex = label.hashCode().let { (it and 0x7FFFFFFF) % notebookColors.size }
    val dotColor = notebookColors[colorIndex]
    Row(
        Modifier.quietClickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(8.dp).clip(RoundedCornerShape(999.dp))
                .background(dotColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotebookIconOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f))
            .quietClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun NotebookNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    forbiddenNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val trimmed = name.trim()
    val valid = trimmed.isNotBlank() && trimmed !in forbiddenNames
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Notebook name" },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        decorationBox = { field -> Box { if (name.isEmpty()) Text("Notebook name", color = MaterialTheme.colorScheme.onSurfaceVariant); field() } }
                    )
                }
                if (trimmed in forbiddenNames) Text("A notebook with this name already exists.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(confirmLabel, { if (valid) onConfirm(trimmed) }, if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f)) },
        dismissButton = { TextButton("Cancel", onDismiss) }
    )
}

@Composable
private fun NotebookMoveDialog(notebooks: List<String>, onDismiss: () -> Unit, onMove: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to notebook") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                notebooks.distinct().forEach { notebook ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .quietClickable { onMove(notebook) }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Text(notebook, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton("Cancel", onDismiss) }
    )
}

@Composable
private fun QuietSearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .28f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search notes" },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { field -> Box { if (query.isEmpty()) Text("Search", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); field() } }
        )
        if (query.isNotEmpty()) {
            QuietIconButton(Icons.Default.Close, "Clear search", modifier = Modifier.size(44.dp)) { onQueryChange("") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReorderableNotesList(
    notes: List<Note>,
    query: String,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    onOpen: (Note) -> Unit,
    onLongPress: (Note) -> Unit,
    onToggleSelection: (Note) -> Unit,
    onReorder: (List<Note>) -> Unit
) {
    var displayedNotes by remember { mutableStateOf(notes) }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var draggedCenter by remember { mutableStateOf(0f) }
    val currentDisplayedNotes = rememberUpdatedState(displayedNotes)
    val listState = rememberLazyListState()

    LaunchedEffect(notes) {
        if (draggedId == null) displayedNotes = notes
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().pointerInput(selectionMode) {
            if (!selectionMode) return@pointerInput
            val handleWidth = 56.dp.toPx()
            var gestureDraggedId: Long? = null
            var gestureNotes = emptyList<Note>()
            var gestureOriginalNotes = emptyList<Note>()
            detectDragGesturesAfterLongPress(
                onDragStart = { start ->
                    if (start.x < size.width - handleWidth) return@detectDragGesturesAfterLongPress
                    val item = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                        start.y >= it.offset && start.y < it.offset + it.size
                    } ?: return@detectDragGesturesAfterLongPress
                    val note = currentDisplayedNotes.value.getOrNull(item.index)
                        ?: return@detectDragGesturesAfterLongPress
                    gestureDraggedId = note.id
                    gestureNotes = currentDisplayedNotes.value
                    gestureOriginalNotes = gestureNotes
                    draggedId = note.id
                    draggedCenter = item.offset + item.size / 2f
                },
                onDrag = drag@ { change, dragAmount ->
                    val activeId = gestureDraggedId ?: return@drag
                    val note = gestureNotes.firstOrNull { it.id == activeId } ?: return@drag
                    change.consume()
                    draggedCenter += dragAmount.y
                    val visibleCenters = listState.layoutInfo.visibleItemsInfo.associate { item ->
                        item.key to (item.offset + item.size / 2f)
                    }
                    var moved = false
                    while (true) {
                        val currentIndex = gestureNotes.indexOfFirst { it.id == activeId }
                        if (currentIndex < 0) return@drag
                        val previousIndex = (currentIndex - 1).takeIf {
                            it >= 0 && gestureNotes[it].pinned == note.pinned
                        }
                        val nextIndex = (currentIndex + 1).takeIf {
                            it < gestureNotes.size && gestureNotes[it].pinned == note.pinned
                        }
                        val targetIndex = when {
                            previousIndex != null && visibleCenters[gestureNotes[previousIndex].id]
                                ?.let { draggedCenter < it } == true -> previousIndex
                            nextIndex != null && visibleCenters[gestureNotes[nextIndex].id]
                                ?.let { draggedCenter > it } == true -> nextIndex
                            else -> currentIndex
                        }
                        if (targetIndex == currentIndex) break
                        gestureNotes = gestureNotes.toMutableList().apply {
                            add(targetIndex, removeAt(currentIndex))
                        }
                        moved = true
                    }
                    if (moved) displayedNotes = gestureNotes
                },
                onDragEnd = {
                    if (gestureNotes.map(Note::id) != gestureOriginalNotes.map(Note::id)) {
                        onReorder(gestureNotes)
                    }
                    gestureDraggedId = null
                    draggedId = null
                    gestureNotes = emptyList()
                    gestureOriginalNotes = emptyList()
                },
                onDragCancel = {
                    gestureDraggedId = null
                    draggedId = null
                    if (gestureOriginalNotes.isNotEmpty()) displayedNotes = gestureOriginalNotes
                    gestureNotes = emptyList()
                    gestureOriginalNotes = emptyList()
                }
            )
        },
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(displayedNotes, key = { note -> note.id }) { note ->
            val isDragging = draggedId == note.id
            NoteRow(
                modifier = if (isDragging) Modifier else Modifier.animateItemPlacement(tween(durationMillis = 110)),
                note = note,
                query = query,
                selected = note.id in selectedIds,
                selectionMode = selectionMode,
                onOpen = { if (selectionMode) onToggleSelection(note) else onOpen(note) },
                onLongPress = { if (!selectionMode) onLongPress(note) else onToggleSelection(note) },
                isDragging = isDragging,
                dragOffset = {
                    if (!isDragging) 0f else {
                        val layoutCenter = listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.key == note.id }
                            ?.let { it.offset + it.size / 2f }
                            ?: draggedCenter
                        draggedCenter - layoutCenter
                    }
                }
            )
            Divider(Modifier.padding(start = 24.dp, end = if (selectionMode) 56.dp else 24.dp), color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun DeletedNotesList(
    notes: List<Note>,
    query: String,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    onOpenActions: (Note) -> Unit,
    onLongPress: (Note) -> Unit,
    onToggleSelection: (Note) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        items(notes, key = { note -> note.id }) { note ->
            NoteRow(
                note = note,
                query = query,
                selected = note.id in selectedIds,
                selectionMode = selectionMode,
                onOpen = { if (selectionMode) onToggleSelection(note) else onOpenActions(note) },
                onLongPress = { if (!selectionMode) onLongPress(note) else onToggleSelection(note) },
                isDragging = false,
                dragOffset = { 0f },
                metadataText = deletedDate(note.body)
            )
            Divider(Modifier.padding(start = 24.dp, end = if (selectionMode) 56.dp else 24.dp), color = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
    note: Note,
    query: String,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    isDragging: Boolean,
    dragOffset: () -> Float,
    modifier: Modifier = Modifier,
    metadataText: String = relativeDate(note.updatedAt)
) {
    Row(
        modifier.fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragOffset()
            }
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f) else Color.Transparent)
            .padding(start = 24.dp, end = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            Modifier.weight(1f).combinedClickable(onClick = onOpen, onLongClick = onLongPress)
                .padding(top = 16.dp, bottom = 15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selected) {
                    Icon(Icons.Default.Check, "Selected", Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(7.dp))
                }
                if (note.pinned) { Icon(Icons.Default.PushPin, "Pinned", Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(6.dp)) }
                Text(
                    highlight(note.title.ifBlank { "Untitled note" }, query, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val visibleBody = visibleNoteBody(note.body)
            val bodyPreview = if (query.isBlank()) visibleBody else searchSnippet(visibleBody, query)
            if (bodyPreview.isNotBlank()) {
                Text(
                    highlight(bodyPreview, query, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(metadataText, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selectionMode) {
            Box(
                Modifier.size(width = 44.dp, height = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DragHandle, "Reorder note", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyState(filtering: Boolean, filter: NoteFilter, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center) {
        val title = when {
            filtering -> "No matching notes"
            filter == NoteFilter.Favorites -> "No favorites yet"
            filter == NoteFilter.RecentlyDeleted -> "No deleted notes"
            else -> "Nothing here yet"
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (!filtering && filter != NoteFilter.RecentlyDeleted) {
            Text(if (filter == NoteFilter.Favorites) "Mark a note as a favorite to find it here." else "Create a note when something is worth keeping.", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NoteCreationSheet(
    onDismiss: () -> Unit,
    onCreateNote: () -> Unit,
    onImport: () -> Unit,
    enabled: Boolean
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .36f)).quietClickable(pressedAlpha = 1f, onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .quietClickable(pressedAlpha = 1f, onClick = {})
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            Box(
                Modifier.padding(top = 10.dp, bottom = 6.dp).align(Alignment.CenterHorizontally).size(width = 34.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            )
            Text(
                stringResource(R.string.create_title),
                Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            CreationSheetAction(
                stringResource(R.string.create_blank_note),
                stringResource(R.string.create_blank_note_description),
                Icons.Default.Description,
                enabled,
                onCreateNote
            )
            Divider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
            CreationSheetAction(stringResource(R.string.create_import), stringResource(R.string.create_import_description), Icons.Default.FileOpen, enabled, onImport)
        }
    }
}

@Composable
private fun TextImportDialog(
    title: String,
    placeholder: String,
    confirmLabel: String,
    isCreating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isCreating) onDismiss() },
        title = { Text(title) },
        text = {
            Column {
                BasicTextField(
                    value = text,
                    onValueChange = { if (!isCreating) text = it },
                    enabled = !isCreating,
                    minLines = 6,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 280.dp).clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f))
                        .padding(14.dp)
                        .semantics { contentDescription = title },
                    decorationBox = { field ->
                        Box {
                            if (text.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            field()
                        }
                    }
                )
                errorMessage?.let {
                    Text(
                        it,
                        Modifier.padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            // Keep the confirm control structurally stable across isCreating toggles.
            // Swapping the whole confirmButton subtree (button <-> progress Row) while the
            // dialog is being torn down triggers recomposition crashes on some OEM ROMs.
            TextButton(
                if (isCreating) stringResource(R.string.creation_in_progress) else confirmLabel,
                onClick = { if (!isCreating) onConfirm(text) }
            )
        },
        dismissButton = { if (!isCreating) TextButton(stringResource(R.string.cancel_action), onDismiss) }
    )
}

@Composable
private fun CreationSheetAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().quietClickable(enabled = enabled, onClick = onClick).padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .66f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(19.dp), tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeletedActionSheet(
    note: Note,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit
) {
    var confirmingDelete by remember(note.id) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .36f)).quietClickable(pressedAlpha = 1f, onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().quietClickable(pressedAlpha = 1f, onClick = {}).padding(bottom = 10.dp)) {
            Box(Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally).size(width = 32.dp, height = 4.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
            Text(note.title.ifBlank { "Untitled note" }, Modifier.padding(24.dp, 18.dp, 24.dp, 10.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            if (confirmingDelete) {
                Text("Permanently delete this note?", Modifier.padding(horizontal = 24.dp, vertical = 16.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.End) {
                    TextButton("Cancel", { confirmingDelete = false })
                    TextButton("Delete", onPermanentlyDelete, MaterialTheme.colorScheme.error)
                }
            } else {
                SheetAction("Restore note", Icons.Default.Restore, onRestore, MaterialTheme.colorScheme.primary)
                SheetAction("Permanently delete", Icons.Default.DeleteOutline, { confirmingDelete = true }, MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ActionSheet(
    note: Note,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onFavorite: () -> Unit,
    notebooks: List<String>,
    onMoveToNotebook: (String) -> Unit,
    onDelete: () -> Unit
) {
    var choosingNotebook by remember(note.id) { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .36f)).quietClickable(pressedAlpha = 1f, onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().quietClickable(pressedAlpha = 1f, onClick = {}).padding(bottom = 10.dp)) {
            Box(Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally).size(width = 32.dp, height = 4.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
            Text(note.title.ifBlank { "Untitled note" }, Modifier.padding(24.dp, 18.dp, 24.dp, 10.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            if (choosingNotebook) {
                SheetAction("Back", Icons.Default.Close, { choosingNotebook = false }, MaterialTheme.colorScheme.primary)
                notebooks.distinct().forEach { notebook ->
                    SheetAction(
                        if (notebook == notebookFromNoteBody(note.body)) "$notebook ✓" else notebook,
                        Icons.Default.Folder,
                        { if (notebook != notebookFromNoteBody(note.body)) onMoveToNotebook(notebook) },
                        if (notebook == notebookFromNoteBody(note.body)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                SheetAction(if (note.pinned) "Unpin note" else "Pin note", Icons.Default.PushPin, onPin)
                SheetAction(if (note.archived) "Remove from favorites" else "Add to favorites", if (note.archived) Icons.Default.Star else Icons.Outlined.StarBorder, onFavorite, if (note.archived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                SheetAction("Move to notebook", Icons.Default.Folder, { choosingNotebook = true }, MaterialTheme.colorScheme.primary)
                val context = androidx.compose.ui.platform.LocalContext.current
                SheetAction("Share note", Icons.Default.Share, {
                    val text = listOf(note.title, visibleNoteBody(note.body)).filter(String::isNotBlank).joinToString("\n\n")
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share note"))
                    onDismiss()
                })
                SheetAction("Delete note", Icons.Default.DeleteOutline, onDelete, MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SheetAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().quietClickable(onClick = action).padding(horizontal = 24.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(20.dp), tint = tint); Spacer(Modifier.width(15.dp)); Text(label, color = tint, style = MaterialTheme.typography.bodyLarge) }
}

private fun matchesSearch(note: Note, query: String): Boolean {
    val searchQuery = query.trim()
    return searchQuery.isBlank() ||
        note.title.contains(searchQuery, ignoreCase = true) ||
        visibleNoteBody(note.body).contains(searchQuery, ignoreCase = true)
}

private fun searchSnippet(text: String, query: String): String {
    val searchQuery = query.trim()
    if (text.isBlank() || searchQuery.isBlank()) return text

    val matchStart = text.indexOf(searchQuery, ignoreCase = true)
    if (matchStart < 0) return text

    val matchEnd = matchStart + searchQuery.length
    var snippetStart = (matchStart - 48).coerceAtLeast(0)
    var snippetEnd = (matchEnd + 64).coerceAtMost(text.length)
    if (snippetStart > 0) {
        text.indexOfFirstWhitespace(snippetStart, matchStart)?.let { snippetStart = it + 1 }
    }
    if (snippetEnd < text.length) {
        text.indexOfLastWhitespace(matchEnd, snippetEnd)?.let { snippetEnd = it }
    }

    val prefix = if (snippetStart > 0) "..." else ""
    val suffix = if (snippetEnd < text.length) "..." else ""
    return prefix + text.substring(snippetStart, snippetEnd).replace(Regex("\\s+"), " ").trim() + suffix
}

private fun String.indexOfFirstWhitespace(startIndex: Int, endIndex: Int): Int? =
    (startIndex until endIndex).firstOrNull { this[it].isWhitespace() }

private fun String.indexOfLastWhitespace(startIndex: Int, endIndex: Int): Int? =
    (startIndex until endIndex).lastOrNull { this[it].isWhitespace() }

private fun highlight(text: String, query: String, background: Color): AnnotatedString = buildAnnotatedString {
    val searchQuery = query.trim()
    if (searchQuery.isBlank()) { append(text); return@buildAnnotatedString }
    var start = 0
    while (start < text.length) {
        val match = text.indexOf(searchQuery, start, ignoreCase = true)
        if (match < 0) { append(text.substring(start)); break }
        append(text.substring(start, match))
        withStyle(SpanStyle(background = background, fontWeight = FontWeight.Medium)) {
            append(text.substring(match, match + searchQuery.length))
        }
        start = match + searchQuery.length
    }
}

private fun relativeDate(time: Long): String = when {
    System.currentTimeMillis() - time < 86_400_000L -> "Today"
    System.currentTimeMillis() - time < 172_800_000L -> "Yesterday"
    else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(time))
}

private fun deletedDate(body: String): String = deletedAtFromNoteBody(body)?.let { deletedAt ->
    when {
        System.currentTimeMillis() - deletedAt < 86_400_000L -> "Deleted today"
        System.currentTimeMillis() - deletedAt < 172_800_000L -> "Deleted yesterday"
        else -> "Deleted ${DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(deletedAt))}"
    }
} ?: "Deletion date unavailable"
