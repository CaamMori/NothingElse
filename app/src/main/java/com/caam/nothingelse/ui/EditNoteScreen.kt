package com.caam.nothingelse.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.caam.nothingelse.data.Note
import kotlinx.coroutines.delay

@Composable
fun EditNoteScreen(
    note: Note,
    onBack: (Note) -> Unit,
    onAutoSave: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onTogglePinned: (Note) -> Unit,
    onToggleFavorite: (Note) -> Unit
) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val favoriteTint by animateColorAsState(
        if (note.archived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(160), label = "favorite-tint"
    )

    fun editedNote() = note.copy(title = title, body = body)

    LaunchedEffect(note.id) { focusRequester.requestFocus() }
    LaunchedEffect(title, body) {
        if (title != note.title || body != note.body) {
            delay(400)
            onAutoSave(editedNote())
        }
    }
    BackHandler { onBack(editedNote()) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onBack(editedNote()) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to notes", tint = MaterialTheme.colorScheme.primary)
            }
            Row {
                IconButton(onClick = { onTogglePinned(editedNote()) }) {
                    Icon(Icons.Default.PushPin, if (note.pinned) "Unpin note" else "Pin note", tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    val text = listOf(title, body).filter(String::isNotBlank).joinToString("\n\n")
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share note"))
                }) {
                    Icon(Icons.Default.Share, "Share note", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onToggleFavorite(editedNote()) }) {
                    AnimatedContent(
                        targetState = note.archived,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                        label = "favorite-icon"
                    ) { favorite ->
                        Icon(
                            if (favorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            if (favorite) "Remove from favorites" else "Add to favorites",
                            tint = favoriteTint
                        )
                    }
                }
                IconButton(onClick = { onDelete(editedNote()) }) {
                    Icon(Icons.Default.DeleteOutline, "Delete note", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        BasicTextField(
            value = title, onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).semantics { contentDescription = "Note title" }.padding(horizontal = 24.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { field -> Box { if (title.isEmpty()) Text("Title", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); field() } }
        )
        BasicTextField(
            value = body, onValueChange = { body = it },
            modifier = Modifier.fillMaxWidth().weight(1f).semantics { contentDescription = "Note body" }.padding(horizontal = 24.dp, vertical = 8.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { field -> Box { if (body.isEmpty()) Text("Start writing", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); field() } }
        )
    }
}
