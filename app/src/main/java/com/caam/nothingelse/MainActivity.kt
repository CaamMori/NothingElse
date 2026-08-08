package com.caam.nothingelse

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissState
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caam.nothingelse.data.Note
import com.caam.nothingelse.ui.EditNoteScreen
import com.caam.nothingelse.ui.theme.NothingElseTheme
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NothingElseTheme { NotesApp() } }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NotesApp(notesViewModel: NotesViewModel = viewModel()) {
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()
    val archivedNotes by notesViewModel.archivedNotes.collectAsStateWithLifecycle()
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var deletingNote by remember { mutableStateOf<Note?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }
    val visibleNotes = if (showArchived) archivedNotes else notes
    val filteredNotes = remember(visibleNotes, query) {
        visibleNotes.filter { it.title.contains(query, true) || it.body.contains(query, true) }
    }
    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh = {
        refreshing = true
        notesViewModel.refresh()
    })

    LaunchedEffect(notes, archivedNotes, refreshing) {
        if (refreshing) refreshing = false
    }

    if (editingNote != null) {
        EditNoteScreen(
            note = editingNote!!,
            onBack = { editingNote = null },
            onAutoSave = notesViewModel::save,
            onDelete = { deletingNote = editingNote },
            onTogglePinned = { notesViewModel.setPinned(editingNote!!, !editingNote!!.pinned) },
            onToggleArchived = { notesViewModel.setArchived(editingNote!!, !editingNote!!.archived) }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pullRefresh(pullRefreshState)
        ) {
            Column(Modifier.fillMaxSize()) {
                HomeHeader(
                    noteCount = visibleNotes.size,
                    showingArchive = showArchived,
                    onArchiveClick = { showArchived = !showArchived }
                )
                AppleSearchBar(query = query, onQueryChange = { query = it })
                if (filteredNotes.isEmpty()) {
                    EmptyState(showingArchive = showArchived, filtering = query.isNotBlank())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            AppleNoteRow(
                                note = note,
                                query = query,
                                onClick = { editingNote = note },
                                onDelete = { deletingNote = note },
                                onTogglePinned = { notesViewModel.setPinned(note, !note.pinned) },
                                onArchive = { notesViewModel.setArchived(note, !note.archived) }
                            )
                        }
                    }
                }
            }
            PullRefreshIndicator(refreshing, pullRefreshState, Modifier.align(Alignment.TopCenter))
            FloatingActionButton(
                onClick = { notesViewModel.createNote { editingNote = it } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).size(56.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, contentDescription = "New note") }
        }
    }

    deletingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { deletingNote = null },
            title = { Text("Delete note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    notesViewModel.delete(note)
                    deletingNote = null
                    editingNote = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingNote = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HomeHeader(noteCount: Int, showingArchive: Boolean, onArchiveClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 28.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(if (showingArchive) "Archive" else "Nothing Else", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(2.dp))
            Text(
                if (showingArchive) "$noteCount archived" else "$noteCount ${if (noteCount == 1) "Note" else "Notes"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onArchiveClick) {
            Icon(if (showingArchive) Icons.Default.Unarchive else Icons.Default.Archive, contentDescription = if (showingArchive) "Show notes" else "Show archive")
        }
    }
}

@Composable
private fun AppleSearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).height(44.dp).clip(RoundedCornerShape(12.dp)),
        placeholder = { Text("Search", style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun EmptyState(showingArchive: Boolean, filtering: Boolean) {
    Box(Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Note, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                when { filtering -> "No matching notes"; showingArchive -> "Nothing in Archive"; else -> "Nothing here yet." },
                style = MaterialTheme.typography.titleMedium
            )
            if (!filtering) Text(
                if (showingArchive) "Archived notes will appear here." else "Create your first note.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun AppleNoteRow(note: Note, query: String, onClick: () -> Unit, onDelete: () -> Unit, onTogglePinned: () -> Unit, onArchive: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dismissState = rememberDismissState(confirmStateChange = { value ->
        if (value == DismissValue.DismissedToStart) onDelete()
        false
    })
    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart),
        background = { DismissBackground(dismissState) },
        dismissContent = {
            Box(
                modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true })
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.pinned) {
                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(highlight(note.title.ifBlank { "Untitled note" }, query), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (note.body.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(highlight(note.body, query), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(relativeDate(note.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)) {
                    DropdownMenuItem(text = { Text(if (note.pinned) "Unpin" else "Pin") }, leadingIcon = { Icon(Icons.Default.PushPin, null) }, onClick = { menuExpanded = false; onTogglePinned() })
                    DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Default.MoreHoriz, null) }, onClick = { menuExpanded = false; onClick() })
                    DropdownMenuItem(text = { Text(if (note.archived) "Restore" else "Archive") }, leadingIcon = { Icon(if (note.archived) Icons.Default.Unarchive else Icons.Default.Archive, null) }, onClick = { menuExpanded = false; onArchive() })
                    ShareMenuItem(note)
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = { menuExpanded = false; onDelete() })
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DismissBackground(dismissState: DismissState) {
    Box(
        modifier = Modifier.fillMaxSize().background(if (dismissState.dismissDirection != null) MaterialTheme.colorScheme.error else Color.Transparent).padding(end = 28.dp),
        contentAlignment = Alignment.CenterEnd
    ) { Icon(Icons.Default.Delete, null, tint = Color.White) }
}

@Composable
private fun ShareMenuItem(note: Note) {
    val context = LocalContext.current
    DropdownMenuItem(
        text = { Text("Share") },
        leadingIcon = { Icon(Icons.Default.Share, null) },
        onClick = {
            val content = listOf(note.title, note.body).filter { it.isNotBlank() }.joinToString("\n\n")
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, content) }, "Share note"))
        }
    )
}

@Composable
private fun highlight(text: String, query: String): AnnotatedString = buildAnnotatedString {
    if (query.isBlank()) { append(text); return@buildAnnotatedString }
    var start = 0
    val lower = text.lowercase()
    val needle = query.lowercase()
    while (true) {
        val index = lower.indexOf(needle, start)
        if (index < 0) { append(text.substring(start)); break }
        append(text.substring(start, index))
        withStyle(MaterialTheme.typography.bodyMedium.toSpanStyle().copy(fontWeight = FontWeight.Bold)) { append(text.substring(index, index + query.length)) }
        start = index + query.length
    }
}

private fun relativeDate(time: Long): String {
    val now = System.currentTimeMillis()
    val day = 24 * 60 * 60 * 1000L
    return when {
        now - time < day -> "Today"
        now - time < day * 2 -> "Yesterday"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(time))
    }
}
