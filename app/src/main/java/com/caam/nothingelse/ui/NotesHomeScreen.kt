package com.caam.nothingelse.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.caam.nothingelse.data.Note
import java.text.DateFormat
import java.util.Date

private enum class NoteFilter { Notes, Favorites }

@Composable
fun NotesHomeScreen(
    notes: List<Note>, favoriteNotes: List<Note>, onOpenNote: (Note) -> Unit,
    onCreateNote: () -> Unit, onDeleteNote: (Note) -> Unit,
    onSetPinned: (Note, Boolean) -> Unit, onSetFavorite: (Note, Boolean) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(NoteFilter.Notes) }
    var actionsFor by remember { mutableStateOf<Note?>(null) }
    val source = if (filter == NoteFilter.Favorites) favoriteNotes else notes
    BackHandler(enabled = actionsFor != null) { actionsFor = null }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().navigationBarsPadding()) {
            HomeHeader(
                filter = filter,
                noteCount = source.size,
                onFilterChange = { filter = it },
                onCreateNote = onCreateNote
            )
            InlineSearch(query, { query = it })
            AnimatedContent(
                targetState = filter,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    EnterTransition.None togetherWith ExitTransition.None
                },
                label = "notes-content"
            ) { selectedFilter ->
                val selectedNotes = if (selectedFilter == NoteFilter.Favorites) favoriteNotes else notes
                val selectedVisible = selectedNotes.filter { it.title.contains(query, true) || it.body.contains(query, true) }
                if (selectedVisible.isEmpty()) {
                    EmptyState(query.isNotBlank(), selectedFilter, Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(selectedVisible, key = { it.id }) { note ->
                            NoteRow(
                                note,
                                query,
                                { onOpenNote(note) },
                                { actionsFor = note }
                            )
                        }
                    }
                }
            }
        }
        AnimatedContent(
            targetState = actionsFor,
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
            },
            label = "note-actions"
        ) { note ->
            if (note != null) {
                ActionSheet(
                    note = note,
                    onDismiss = { actionsFor = null },
                    onPin = { onSetPinned(note, !note.pinned); actionsFor = null },
                    onFavorite = { onSetFavorite(note, !note.archived); actionsFor = null },
                    onDelete = { onDeleteNote(note); actionsFor = null }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(filter: NoteFilter, noteCount: Int, onFilterChange: (NoteFilter) -> Unit, onCreateNote: () -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 12.dp, top = 30.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (filter == NoteFilter.Favorites) "Favorites" else "Notes",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$noteCount ${if (noteCount == 1) "note" else "notes"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onCreateNote) {
                Icon(Icons.Default.Add, "New note", tint = MaterialTheme.colorScheme.primary)
            }
        }
        FilterControl(filter, onFilterChange, Modifier.padding(top = 20.dp, end = 12.dp))
    }
}

@Composable
private fun FilterControl(selected: NoteFilter, onSelect: (NoteFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp)
    ) {
        FilterOption("Notes", selected == NoteFilter.Notes) { onSelect(NoteFilter.Notes) }
        FilterOption("Favorites", selected == NoteFilter.Favorites) { onSelect(NoteFilter.Favorites) }
    }
}

@Composable
private fun FilterOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(7.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InlineSearch(query: String, onQueryChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search notes" },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { field -> Box { if (query.isEmpty()) Text("Search notes", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); field() } }
        )
    }
}

@Composable
private fun NoteRow(note: Note, query: String, onOpen: () -> Unit, onActions: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.pinned) { Icon(Icons.Default.PushPin, "Pinned", Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(6.dp)) }
                Text(highlight(note.title.ifBlank { "Untitled note" }, query), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (note.body.isNotBlank()) Text(highlight(note.body, query), Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(relativeDate(note.updatedAt), Modifier.padding(top = 7.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onActions) { Icon(Icons.Default.MoreHoriz, "Note actions", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun EmptyState(filtering: Boolean, filter: NoteFilter, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(if (filtering) "No matching notes" else if (filter == NoteFilter.Favorites) "No favorites yet" else "Nothing here yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (!filtering) Text(if (filter == NoteFilter.Favorites) "Mark a note as a favorite to find it here." else "Create a note when something is worth keeping.", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionSheet(note: Note, onDismiss: () -> Unit, onPin: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .36f)).clickable(onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding().clickable(onClick = {}).padding(bottom = 10.dp)) {
            Box(Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally).size(width = 32.dp, height = 4.dp).background(MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
            Text(note.title.ifBlank { "Untitled note" }, Modifier.padding(24.dp, 18.dp, 24.dp, 10.dp), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            SheetAction(if (note.pinned) "Unpin note" else "Pin note", Icons.Default.PushPin, onPin)
            SheetAction(if (note.archived) "Remove from favorites" else "Add to favorites", if (note.archived) Icons.Default.Star else Icons.Outlined.StarBorder, onFavorite, if (note.archived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            val context = androidx.compose.ui.platform.LocalContext.current
            SheetAction("Share note", Icons.Default.Share, {
                val text = listOf(note.title, note.body).filter(String::isNotBlank).joinToString("\n\n")
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share note"))
                onDismiss()
            })
            SheetAction("Delete note", Icons.Default.DeleteOutline, onDelete, MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SheetAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().clickable(onClick = action).padding(horizontal = 24.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(20.dp), tint = tint); Spacer(Modifier.width(15.dp)); Text(label, color = tint, style = MaterialTheme.typography.bodyLarge) }
}

private fun highlight(text: String, query: String): AnnotatedString = buildAnnotatedString {
    if (query.isBlank()) { append(text); return@buildAnnotatedString }
    var start = 0
    while (start < text.length) {
        val match = text.indexOf(query, start, ignoreCase = true)
        if (match < 0) { append(text.substring(start)); break }
        append(text.substring(start, match)); withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(match, match + query.length)) }; start = match + query.length
    }
}

private fun relativeDate(time: Long): String = when {
    System.currentTimeMillis() - time < 86_400_000L -> "Today"
    System.currentTimeMillis() - time < 172_800_000L -> "Yesterday"
    else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(time))
}
