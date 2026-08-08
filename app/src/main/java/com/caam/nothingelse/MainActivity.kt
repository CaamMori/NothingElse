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
        setContent { NothingElseTheme { NothingElseApp() } }
    }
}

@Composable
private fun NothingElseApp(notesViewModel: NotesViewModel = viewModel()) {
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()
    val favorites by notesViewModel.favoriteNotes.collectAsStateWithLifecycle()
    var openNote by remember { mutableStateOf<Note?>(null) }

    openNote?.let { note ->
        EditNoteScreen(
            note = note,
            onBack = { editedNote ->
                notesViewModel.save(editedNote)
                openNote = null
            },
            onAutoSave = { editedNote ->
                openNote = editedNote
                notesViewModel.save(editedNote)
            },
            onDelete = { noteToDelete ->
                notesViewModel.delete(noteToDelete)
                openNote = null
            },
            onTogglePinned = { editedNote ->
                openNote = editedNote.copy(pinned = !editedNote.pinned)
                notesViewModel.setPinned(editedNote, !editedNote.pinned)
            },
            onToggleFavorite = { editedNote ->
                notesViewModel.setFavorite(editedNote, !editedNote.archived)
            }
        )
    } ?: NotesHomeScreen(
        notes = notes,
        favoriteNotes = favorites,
        onOpenNote = { openNote = it },
        onCreateNote = { notesViewModel.create { openNote = it } },
        onDeleteNote = notesViewModel::delete,
        onSetPinned = notesViewModel::setPinned,
        onSetFavorite = notesViewModel::setFavorite
    )
}
