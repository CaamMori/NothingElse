package com.caam.nothingelse

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.caam.nothingelse.data.Note
import com.caam.nothingelse.ui.EditNoteScreen
import com.caam.nothingelse.ui.NotesHomeScreen
import com.caam.nothingelse.ui.DEFAULT_NOTEBOOK_NAME
import com.caam.nothingelse.ui.theme.NothingElseTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate so the system splash uses our
        // icon_background color and avoids the light fringe around the logo.
        installSplashScreen()
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
    val deletedNotes by notesViewModel.deletedNotes.collectAsStateWithLifecycle()
    val notebooks by notesViewModel.notebooks.collectAsStateWithLifecycle()
    val creationState by notesViewModel.creationState.collectAsStateWithLifecycle()
    var editorNote by remember { mutableStateOf<Note?>(null) }
    var isEditorClosing by remember { mutableStateOf(false) }
    val editorOffset = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun closeEditor() {
        if (isEditorClosing) return
        isEditorClosing = true
        scope.launch {
            editorOffset.animateTo(1f, tween(300))
            editorNote = null
            isEditorClosing = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        NotesHomeScreen(
            notes = notes,
            favoriteNotes = favorites,
            deletedNotes = deletedNotes,
            notebooks = notebooks,
            onOpenNote = { note ->
                if (editorNote != null) return@NotesHomeScreen
                scope.launch {
                    editorOffset.snapTo(1f)
                    editorNote = note
                    withFrameNanos { }
                    editorOffset.animateTo(0f, tween(300))
                }
            },
            isCreatingNote = creationState.isCreating,
            onCreateBlank = { notebook, onComplete ->
                notesViewModel.createBlank(notebook.ifBlank { DEFAULT_NOTEBOOK_NAME }, onComplete)
            },
            onCreateNotebook = notesViewModel::createNotebook,
            onCreateFromImport = { notebook, content, onComplete ->
                notesViewModel.createFromImport(notebook.ifBlank { DEFAULT_NOTEBOOK_NAME }, content, onComplete)
            },
            onDeleteNote = notesViewModel::delete,
            onDeleteNotes = notesViewModel::deleteNotes,
            onRestoreNote = notesViewModel::restoreNote,
            onRestoreNotes = notesViewModel::restoreNotes,
            onPermanentlyDeleteNote = notesViewModel::permanentlyDelete,
            onPermanentlyDeleteNotes = notesViewModel::permanentlyDeleteNotes,
            onReorderNotes = notesViewModel::reorder,
            onSetNotebook = notesViewModel::setNotebook,
            onRenameNotebook = notesViewModel::renameNotebook,
            onDeleteNotebook = notesViewModel::deleteNotebook,
            onSetPinned = notesViewModel::setPinned,
            onSetFavorite = notesViewModel::setFavorite
        )

        editorNote?.let { note ->
            Box(
                Modifier
                    .fillMaxSize()
                    .offset {
                        IntOffset(
                            (with(density) { maxWidth.roundToPx() } * editorOffset.value).roundToInt(),
                            0
                        )
                    }
                    .background(MaterialTheme.colorScheme.background)
            ) {
                EditNoteScreen(
                    note = note,
                    notebooks = notebooks,
                    onSaveAndExit = { editedNote ->
                        notesViewModel.save(editedNote, ::closeEditor)
                    },
                    onExit = ::closeEditor,
                    onDelete = { noteToDelete ->
                        notesViewModel.delete(noteToDelete)
                        closeEditor()
                    },
                    onSetNotebook = { editedNote, notebook ->
                        editorNote = editedNote
                        notesViewModel.setNotebook(editedNote, notebook)
                    },
                    onTogglePinned = { editedNote ->
                        editorNote = editedNote.copy(pinned = !editedNote.pinned)
                        notesViewModel.setPinned(editedNote, !editedNote.pinned)
                    },
                    onToggleFavorite = { editedNote ->
                        editorNote = editedNote.copy(archived = !editedNote.archived)
                        notesViewModel.setFavorite(editedNote, !editedNote.archived)
                    }
                )
            }
        }
    }
}
