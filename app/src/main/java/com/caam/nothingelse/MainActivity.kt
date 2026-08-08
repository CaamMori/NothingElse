package com.caam.nothingelse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caam.nothingelse.data.Note
import com.caam.nothingelse.ui.EditNoteScreen
import com.caam.nothingelse.ui.NotesHomeScreen
import com.caam.nothingelse.ui.theme.NothingElseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NothingElseTheme {
                NothingElseApp()
            }
        }
    }
}

@Composable
private fun NothingElseApp(notesViewModel: NotesViewModel = viewModel()) {
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()
    val archivedNotes by notesViewModel.archivedNotes.collectAsStateWithLifecycle()
    var openNote by remember { mutableStateOf<Note?>(null) }

    openNote?.let { note ->
        EditNoteScreen(
            note = note,
            onBack = { openNote = null },
            onAutoSave = notesViewModel::save,
            onDelete = {
                notesViewModel.delete(note)
                openNote = null
            },
            onTogglePinned = { notesViewModel.setPinned(note, !note.pinned) },
            onToggleArchived = {
                notesViewModel.setArchived(note, !note.archived)
                openNote = null
            }
        )
    } ?: NotesHomeScreen(
        notes = notes,
        archivedNotes = archivedNotes,
        onOpenNote = { openNote = it },
        onCreateNote = { notesViewModel.createNote { openNote = it } },
        onSaveNote = notesViewModel::save,
        onDeleteNote = notesViewModel::delete,
        onSetPinned = notesViewModel::setPinned,
        onSetArchived = notesViewModel::setArchived,
        onRefresh = notesViewModel::refresh
    )
}
