package com.caam.nothingelse.ui

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberDismissState
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
import com.caam.nothingelse.data.Note
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NotesHomeScreen(
    notes: List<Note>,
    archivedNotes: List<Note>,
    onOpenNote: (Note) -> Unit,
    onCreateNote: () -> Unit,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onSetPinned: (Note, Boolean) -> Unit,
    onSetArchived: (Note, Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showArchive by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Note?>(null) }
    var renameTarget by remember { mutableStateOf<Note?>(null) }
    var renamedTitle by remember { mutableStateOf("") }
    val displayedNotes = if (showArchive) archivedNotes else notes
    val filteredNotes = displayedNotes.filter {
        it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
    }
    val refreshState = rememberPullRefreshState(refreshing, onRefresh = {
        refreshing = true
        onRefresh()
    })

    LaunchedEffect(notes, archivedNotes) {
        if (refreshing) refreshing = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pullRefresh(refreshState)
    ) {
        Column(Modifier.fillMaxSize()) {
            HomeTitle(
                noteCount = displayedNotes.size,
                newestUpdate = displayedNotes.maxOfOrNull { it.updatedAt },
                showingArchive = showArchive,
                onArchiveClick = { showArchive = !showArchive }
            )
            SearchField(query = query, onQueryChange = { query = it })
            AnimatedContent(
                targetState = filteredNotes.isEmpty(),
                transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(90)) },
                label = "notes content"
            ) { isEmpty ->
                if (isEmpty) {
                    EmptyNotes(
                        filtering = query.isNotBlank(),
                        archived = showArchive,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 92.dp)
                    ) {
                        items(filteredNotes, key = { it.id }) { note ->
                            NoteRow(
                                note = note,
                                query = query,
                                onOpen = { onOpenNote(note) },
                                onRename = { renameTarget = note; renamedTitle = note.title },
                                onDelete = { deleteTarget = note },
                                onTogglePinned = { onSetPinned(note, !note.pinned) },
                                onArchive = { onSetArchived(note, !note.archived) }
                            )
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(refreshing, refreshState, Modifier.align(Alignment.TopCenter))
        FloatingActionButton(
            onClick = onCreateNote,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create note")
        }
    }

    deleteTarget?.let { note ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { onDeleteNote(note); deleteTarget = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
    renameTarget?.let { note ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename note") },
            text = {
                TextField(
                    value = renamedTitle,
                    onValueChange = { renamedTitle = it },
                    placeholder = { Text("Title") },
                    singleLine = true,
                    colors = textFieldColors()
                )
            },
            confirmButton = {
                TextButton(onClick = { onSaveNote(note.copy(title = renamedTitle)); renameTarget = null }) {
                    Text("Rename")
                }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun HomeTitle(noteCount: Int, newestUpdate: Long?, showingArchive: Boolean, onArchiveClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 10.dp, top = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(if (showingArchive) "Archive" else "Nothing Else", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(3.dp))
            Text(
                noteLabel(noteCount, newestUpdate, showingArchive),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onArchiveClick) {
            Icon(
                if (showingArchive) Icons.Default.Unarchive else Icons.Default.Archive,
                contentDescription = if (showingArchive) "Show active notes" else "Show archived notes",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp)),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        placeholder = { Text("Search") },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = textFieldColors()
    )
}

@Composable
private fun EmptyNotes(filtering: Boolean, archived: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Note, contentDescription = null, modifier = Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(if (filtering) "No matching notes" else if (archived) "Nothing in Archive" else "Nothing here yet.", style = MaterialTheme.typography.titleMedium)
            if (!filtering) {
                Spacer(Modifier.height(3.dp))
                Text(if (archived) "Archived notes will appear here." else "Create your first note.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
private fun NoteRow(
    note: Note,
    query: String,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePinned: () -> Unit,
    onArchive: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val dismissState = rememberDismissState(confirmStateChange = {
        if (it == DismissValue.DismissedToStart) onDelete()
        false
    })
    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart),
        background = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error).padding(end = 28.dp),
                contentAlignment = Alignment.CenterEnd
            ) { Icon(Icons.Default.Delete, contentDescription = "Delete note", tint = Color.White) }
        },
        dismissContent = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onOpen, onLongClick = { menuOpen = true })
            ) {
                Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 13.dp, bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.pinned) {
                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(highlight(note.title.ifBlank { "Untitled note" }, query), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (note.body.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(highlight(note.body, query), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(relativeDate(note.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)))
                NoteActions(
                    expanded = menuOpen,
                    note = note,
                    onDismiss = { menuOpen = false },
                    onPin = { menuOpen = false; onTogglePinned() },
                    onRename = { menuOpen = false; onRename() },
                    onArchive = { menuOpen = false; onArchive() },
                    onDelete = { menuOpen = false; onDelete() },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
                )
            }
        }
    )
}

@Composable
private fun NoteActions(
    expanded: Boolean,
    note: Note,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onRename: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = modifier) {
        DropdownMenuItem(text = { Text(if (note.pinned) "Unpin" else "Pin") }, leadingIcon = { Icon(Icons.Default.PushPin, null) }, onClick = onPin)
        DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = onRename)
        DropdownMenuItem(text = { Text(if (note.archived) "Restore" else "Archive") }, leadingIcon = { Icon(if (note.archived) Icons.Default.Unarchive else Icons.Default.Archive, null) }, onClick = onArchive)
        DropdownMenuItem(
            text = { Text("Share") },
            leadingIcon = { Icon(Icons.Default.Share, null) },
            onClick = {
                onDismiss()
                val text = listOf(note.title, note.body).filter(String::isNotBlank).joinToString("\n\n")
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share note"))
            }
        )
        DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }, onClick = onDelete)
    }
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Composable
private fun highlight(text: String, query: String): AnnotatedString = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    var start = 0
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    while (true) {
        val index = lowerText.indexOf(lowerQuery, start)
        if (index < 0) {
            append(text.substring(start))
            break
        }
        append(text.substring(start, index))
        withStyle(MaterialTheme.typography.bodyMedium.toSpanStyle().copy(fontWeight = FontWeight.Bold)) {
            append(text.substring(index, index + query.length))
        }
        start = index + query.length
    }
}

private fun relativeDate(time: Long): String {
    val day = 24 * 60 * 60 * 1000L
    return when {
        System.currentTimeMillis() - time < day -> "Today"
        System.currentTimeMillis() - time < day * 2 -> "Yesterday"
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(time))
    }
}

private fun noteLabel(count: Int, newestUpdate: Long?, archived: Boolean): String {
    val countLabel = if (archived) "$count archived" else "$count ${if (count == 1) "note" else "notes"}"
    return newestUpdate?.let { "$countLabel  |  Updated ${relativeDate(it)}" } ?: countLabel
}
