package com.caam.nothingelse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caam.nothingelse.data.Note
import com.caam.nothingelse.ui.EditNoteScreen
import com.caam.nothingelse.ui.theme.NothingElseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NothingElseTheme {
                NotesApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesApp(notesViewModel: NotesViewModel = viewModel()) {
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()
    val archivedNotes by notesViewModel.archivedNotes.collectAsStateWithLifecycle()
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }

    if (editingNote != null) {
        EditNoteScreen(
            note = editingNote!!,
            onBack = { editingNote = null },
            onSave = { note ->
                notesViewModel.save(note)
                editingNote = null
            },
            onDelete = { showDeleteConfirmation = true }
        )
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete note?") },
                text = { Text("This note will be permanently deleted.") },
                confirmButton = {
                    TextButton(onClick = {
                        notesViewModel.delete(editingNote!!)
                        showDeleteConfirmation = false
                        editingNote = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
                }
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showArchived) "Archive" else "NothingElse") },
                actions = {
                    IconButton(onClick = { showArchived = !showArchived }) {
                        Icon(
                            imageVector = if (showArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (showArchived) "Show notes" else "Show archive"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingNote = Note(id = 0) }) {
                Icon(Icons.Default.Add, contentDescription = "New note")
            }
        }
    ) { padding ->
        val visibleNotes = if (showArchived) archivedNotes else notes
        if (visibleNotes.isEmpty()) {
            EmptyState(padding, showArchived)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(visibleNotes, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        onClick = { editingNote = note },
                        onTogglePinned = { notesViewModel.setPinned(note, !note.pinned) },
                        onArchive = { notesViewModel.setArchived(note, !note.archived) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues, showingArchive: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (showingArchive) "Archive is empty" else "No notes yet", style = MaterialTheme.typography.headlineSmall)
            Text(
                if (showingArchive) "Archived notes will appear here." else "Tap + to write your first note.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoteRow(note: Note, onClick: () -> Unit, onTogglePinned: () -> Unit, onArchive: () -> Unit) {
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        headlineContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (note.pinned) Icon(Icons.Default.PushPin, contentDescription = "Pinned")
                Text(note.title.ifBlank { "Untitled note" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        supportingContent = {
            Text(note.body.ifBlank { "No content" }, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            Row {
                IconButton(onClick = onTogglePinned) {
                    Icon(Icons.Default.PushPin, contentDescription = if (note.pinned) "Unpin" else "Pin")
                }
                IconButton(onClick = onArchive) {
                    Icon(
                        imageVector = if (note.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = if (note.archived) "Restore" else "Archive"
                    )
                }
            }
        }
    )
}
