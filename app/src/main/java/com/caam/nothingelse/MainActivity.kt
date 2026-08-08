package com.caam.nothingelse

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent { NothingElseTheme { NothingElseApp() } }
    }
}

@Composable
private fun NothingElseApp(notesViewModel: NotesViewModel = viewModel()) {
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()
    val favorites by notesViewModel.favoriteNotes.collectAsStateWithLifecycle()
    var openNote by remember { mutableStateOf<Note?>(null) }

    AnimatedContent(
        targetState = openNote,
        contentKey = { it?.id ?: -1L },
        transitionSpec = {
            if (targetState != null) {
                slideIntoContainer(SlideDirection.Left, animationSpec = tween(300)) togetherWith
                    ExitTransition.None
            } else {
                EnterTransition.None togetherWith
                    slideOutOfContainer(SlideDirection.Right, animationSpec = tween(300))
            }
        },
        label = "note-navigation"
    ) { note ->
        if (note != null) {
            EditNoteScreen(
                note = note,
                onSaveAndExit = { editedNote ->
                    notesViewModel.save(editedNote) { openNote = null }
                },
                onExit = { openNote = null },
                onDelete = { noteToDelete ->
                    notesViewModel.delete(noteToDelete)
                    openNote = null
                },
                onTogglePinned = { editedNote ->
                    openNote = editedNote.copy(pinned = !editedNote.pinned)
                    notesViewModel.setPinned(editedNote, !editedNote.pinned)
                },
                onToggleFavorite = { editedNote ->
                    openNote = editedNote.copy(archived = !editedNote.archived)
                    notesViewModel.setFavorite(editedNote, !editedNote.archived)
                }
            )
        } else {
            NotesHomeScreen(
                notes = notes,
                favoriteNotes = favorites,
                onOpenNote = { openNote = it },
                onCreateNote = { notesViewModel.create { openNote = it } },
                onDeleteNote = notesViewModel::delete,
                onSetPinned = notesViewModel::setPinned,
                onSetFavorite = notesViewModel::setFavorite
            )
        }
    }
}
