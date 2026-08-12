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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caam.nothingelse.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ParagraphFocusRequest(
    val paragraphId: String,
    val cursorOffset: Int,
    // Monotonic token: two consecutive requests with the same id+offset must still fire,
    // otherwise LaunchedEffect dedupes them and the caret silently stays behind.
    val token: Long
)

private fun paragraphFieldValue(text: String, cursorOffset: Int = text.length): TextFieldValue =
    TextFieldValue(
        text = text,
        selection = TextRange(cursorOffset.coerceIn(0, text.length))
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditNoteScreen(
    note: Note,
    notebooks: List<String>,
    onSaveAndExit: (Note) -> Unit,
    onExit: () -> Unit,
    onDelete: (Note) -> Unit,
    onSetNotebook: (Note, String) -> Unit,
    onTogglePinned: (Note) -> Unit,
    onToggleFavorite: (Note) -> Unit
) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var paragraphs by remember(note.id) { mutableStateOf(parseNoteBody(note.body)) }
    var savedTitle by remember(note.id) { mutableStateOf(note.title) }
    var savedBody by remember(note.id) { mutableStateOf(note.body) }
    var currentNotebook by remember(note.id) { mutableStateOf(notebookFromNoteBody(note.body)) }
    var currentParagraphId by remember(note.id) { mutableStateOf<String?>(null) }
    var requestedParagraphFocus by remember(note.id) { mutableStateOf<ParagraphFocusRequest?>(null) }
    var focusRequestToken by remember(note.id) { mutableStateOf(0L) }
    var showFormatPanel by remember { mutableStateOf(false) }
    var showNotebookPicker by remember { mutableStateOf(false) }
    var showMoreActions by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var pendingProtectedAction by remember(note.id) { mutableStateOf<(() -> Unit)?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveStatus by remember(note.id) { mutableStateOf("Saved") }
    val context = LocalContext.current
    val visibleBodyText = paragraphs.joinToString("\n", transform = NoteParagraph::text)
    val visibleCharacterCount = visibleBodyText.count { !it.isWhitespace() }
    val metadataPrefix = "${formatNoteTimestamp(note.createdAt)} | ${visibleCharacterCount} chars"
    val favoriteTint by animateColorAsState(
        if (note.archived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(160), label = "favorite-tint"
    )

    fun currentBody() = setNoteBodyNotebook(
        replaceNoteBodyContent(note.body, serializeNoteBody(paragraphs)),
        currentNotebook
    )
    fun hasUnsavedChanges() = title != savedTitle || currentBody() != savedBody

    fun saveAndExit() {
        val body = currentBody()
        if (title == savedTitle && body == savedBody) {
            onExit()
        } else if (!isSaving) {
            isSaving = true
            saveStatus = "Saving…"
            onSaveAndExit(note.copy(title = title, body = body))
        }
    }

    fun requestExit() {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onExit()
        }
    }

    fun discardAndExit() {
        showUnsavedChangesDialog = false
        onExit()
    }

    fun currentNote() = note.copy(title = title, body = currentBody())

    fun resetEditsToSaved() {
        title = savedTitle
        paragraphs = parseNoteBody(savedBody)
        currentNotebook = notebookFromNoteBody(savedBody)
    }

    // Guard favorite/pin/delete against unsaved edits: prompt Save/Discard/Cancel,
    // then operate on the CURRENT edited content instead of the stale incoming note.
    fun protectedAction(action: () -> Unit) {
        if (hasUnsavedChanges()) pendingProtectedAction = action else action()
    }

    // Always resolve a paragraph by its runtime id at call time. Indexes captured in
    // composition closures go stale as soon as a split/merge changes the list.
    fun indexOfParagraphId(id: String?): Int =
        if (id == null) -1 else paragraphs.indexOfFirst { it.id == id }

    fun focusedParagraphIndex(): Int =
        indexOfParagraphId(currentParagraphId).takeIf { it >= 0 } ?: 0

    fun requestCaret(paragraphId: String, cursorOffset: Int) {
        focusRequestToken += 1
        currentParagraphId = paragraphId
        requestedParagraphFocus = ParagraphFocusRequest(paragraphId, cursorOffset, focusRequestToken)
    }

    fun updateCurrent(transform: (NoteParagraph) -> NoteParagraph) {
        paragraphs = paragraphs.toMutableList().also { list ->
            if (list.isEmpty()) return@also
            val index = focusedParagraphIndex().coerceIn(list.indices)
            list[index] = transform(list[index])
        }
    }

    fun shareCurrentNote() {
        val text = listOf(title, visibleNoteBody(serializeNoteBody(paragraphs))).filter(String::isNotBlank).joinToString("\n\n")
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "Share note"))
    }

    LaunchedEffect(title, paragraphs, currentNotebook) { saveStatus = if (hasUnsavedChanges()) "Edited" else "Saved" }
    BackHandler { requestExit() }
    // Title + metadata already live OUTSIDE the scrollable body below. imePadding keeps the
    // whole editor resized (not panned) when the keyboard opens, so the title bar is never
    // pushed off-screen and the format toolbar stays reachable above the keyboard.
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            QuietIconButton(onClick = ::requestExit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to notes", tint = MaterialTheme.colorScheme.primary)
            }
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.Center
            ) {
                QuietIconButton(onClick = {
                    protectedAction {
                        onToggleFavorite(currentNote())
                        savedTitle = title
                        savedBody = currentBody()
                    }
                }) {
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
                Box {
                    QuietIconButton(onClick = { showMoreActions = true }) {
                        Icon(Icons.Default.MoreHoriz, "More note actions", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMoreActions, onDismissRequest = { showMoreActions = false }) {
                        DropdownMenuItem(
                            text = { Text(if (note.pinned) "Unpin note" else "Pin note") },
                            leadingIcon = { Icon(Icons.Default.PushPin, null) },
                            onClick = {
                                showMoreActions = false
                                protectedAction {
                                    onTogglePinned(currentNote())
                                    savedTitle = title
                                    savedBody = currentBody()
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share note") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            onClick = { showMoreActions = false; shareCurrentNote() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete note", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMoreActions = false
                                protectedAction { onDelete(currentNote()) }
                            }
                        )
                    }
                }
                TextButton(onClick = ::saveAndExit) {
                    Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        BasicTextField(
            value = title, onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Note title" }.padding(horizontal = 24.dp, vertical = 10.dp),
            textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface, lineHeight = 38.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { field -> Box { if (title.isEmpty()) Text("Title", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); field() } }
        )
        MetadataBlock(
            metadata = metadataPrefix,
            notebook = currentNotebook,
            saveStatus = saveStatus,
            onNotebookClick = { showNotebookPicker = true },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
        )
        Spacer(Modifier.height(18.dp))
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .semantics { contentDescription = "Note body" }
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            val showBodyPlaceholder = paragraphs.all { it.text.isBlank() }
            fun focusBodyEnd() {
                val last = paragraphs.lastOrNull() ?: return
                requestCaret(last.id, last.text.length)
            }
            paragraphs.forEachIndexed { index, paragraph ->
                key(paragraph.id) {
                val paragraphFocusRequester = remember { FocusRequester() }
                ParagraphEditor(
                    paragraph = paragraph,
                    showPlaceholder = showBodyPlaceholder && index == 0,
                    listNumber = numberedListOrdinal(paragraphs, index),
                    focusRequester = paragraphFocusRequester,
                    requestedFocus = requestedParagraphFocus?.takeIf { it.paragraphId == paragraph.id },
                    onFocusRequestHandled = { requestedParagraphFocus = null },
                    onFocused = { currentParagraphId = paragraph.id },
                    onCheckedChange = {
                        val at = indexOfParagraphId(paragraph.id)
                        if (at >= 0) {
                            paragraphs = paragraphs.toMutableList().also { list ->
                                list[at] = list[at].copy(completed = !list[at].completed)
                            }
                        }
                    },
                    onTextChange = { value, cursorOffset ->
                        val at = indexOfParagraphId(paragraph.id)
                        if (at >= 0) {
                            val result = applyParagraphTextChange(paragraphs[at], value, cursorOffset)
                            paragraphs = paragraphs.toMutableList().also { list ->
                                list[at] = result.paragraphs.first()
                                result.paragraphs.drop(1).forEachIndexed { offset, changedParagraph ->
                                    list.add(at + offset + 1, changedParagraph)
                                }
                            }
                            if (result.paragraphs.size > 1) {
                                val target = result.paragraphs[
                                    result.cursorParagraphOffset.coerceIn(result.paragraphs.indices)
                                ]
                                requestCaret(target.id, result.cursorTextOffset)
                            }
                        }
                    },
                    onBoundaryDelete = { deleteBackward ->
                        val at = indexOfParagraphId(paragraph.id)
                        if (at < 0) return@ParagraphEditor false
                        val change = applyParagraphBoundaryDeletion(paragraphs, at, deleteBackward)
                            ?: return@ParagraphEditor false
                        val survivor = change.paragraphs[change.focusedParagraph]
                        paragraphs = change.paragraphs
                        requestCaret(survivor.id, change.cursorOffset)
                        true
                    }
                )
                }
            }
            Spacer(
                Modifier.fillMaxWidth()
                    .height(120.dp)
                    .quietClickable(pressedAlpha = 1f, onClick = { showFormatPanel = false; focusBodyEnd() })
            )
        }
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showFormatPanel) {
                val formatParagraph = paragraphs[focusedParagraphIndex().coerceIn(paragraphs.indices)]
                FormatPanel(
                    paragraph = formatParagraph,
                    onUpdate = { transform ->
                        val index = indexOfParagraphId(formatParagraph.id)
                        if (index >= 0) {
                            paragraphs = paragraphs.toMutableList().also { list ->
                                list[index] = transform(list[index])
                            }
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(
                Modifier.clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuietIconButton(
                    onClick = {
                        updateCurrent { paragraph ->
                            paragraph.copy(
                                todo = !paragraph.todo,
                                completed = false,
                                listStyle = ParagraphListStyle.NONE
                            )
                        }
                    }
                ) {
                    Icon(Icons.Default.CheckBoxOutlineBlank, "Toggle todo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                QuietIconButton(onClick = { showFormatPanel = !showFormatPanel }) {
                    Icon(
                        Icons.Default.FormatSize,
                        if (showFormatPanel) "Close formatting" else "Format paragraph",
                        tint = if (showFormatPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    if (showNotebookPicker) {
        NotebookPickerDialog(
            notebooks = notebooks,
            selected = currentNotebook,
            onDismiss = { showNotebookPicker = false },
            onSelect = { notebook ->
                val movedNote = note.copy(body = setNoteBodyNotebook(note.body, notebook))
                currentNotebook = notebook
                savedBody = setNoteBodyNotebook(savedBody, notebook)
                onSetNotebook(movedNote, notebook)
                showNotebookPicker = false
            }
        )
    }
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Save changes?") },
            text = { Text("Your note has unsaved changes.") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedChangesDialog = false
                    saveAndExit()
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showUnsavedChangesDialog = false }) { Text("Cancel") }
                    TextButton(onClick = ::discardAndExit) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }
    if (pendingProtectedAction != null) {
        val action = pendingProtectedAction!!
        AlertDialog(
            onDismissRequest = { pendingProtectedAction = null },
            title = { Text("Save changes?") },
            text = { Text("Save your edits before continuing?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingProtectedAction = null
                    if (!isSaving) {
                        savedTitle = title
                        savedBody = currentBody()
                        onSaveAndExit(currentNote())
                        action()
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingProtectedAction = null }) { Text("Cancel") }
                    TextButton(onClick = {
                        pendingProtectedAction = null
                        resetEditsToSaved()
                        action()
                    }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                }
            }
        )
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
    return alpha(if (enabled && pressed) pressedAlpha else 1f)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
private fun QuietIconButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier.size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .quietClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataBlock(metadata: String, notebook: String, saveStatus: String, onNotebookClick: () -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalArrangement = Arrangement.Start) {
        Text(metadata, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("  •  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(
            notebook,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).quietClickable(onClick = onNotebookClick).padding(horizontal = 3.dp, vertical = 4.dp).align(Alignment.CenterVertically),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary
        )
        Text("  •  $saveStatus", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotebookPickerDialog(notebooks: List<String>, selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to notebook") },
        text = {
            Column {
                notebooks.distinct().forEach { notebook ->
                    Text(
                        notebook,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).quietClickable { onSelect(notebook) }.padding(vertical = 12.dp, horizontal = 6.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (notebook == selected) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (notebook == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = { Text("Done", Modifier.clip(RoundedCornerShape(8.dp)).quietClickable(onClick = onDismiss).padding(12.dp), color = MaterialTheme.colorScheme.primary) }
    )
}

private fun formatNoteTimestamp(timestamp: Long): String = SimpleDateFormat("yyyy/M/d HH:mm", Locale.getDefault()).format(Date(timestamp))

internal data class ParagraphBoundaryChange(
    val paragraphs: List<NoteParagraph>,
    val focusedParagraph: Int,
    val cursorOffset: Int
)

internal data class ParagraphSplitResult(
    val paragraphs: List<NoteParagraph>,
    val cursorParagraphOffset: Int,
    val cursorTextOffset: Int
)

internal fun applyParagraphTextChange(
    paragraph: NoteParagraph,
    text: String,
    cursorOffset: Int
): ParagraphSplitResult {
    val lines = text.split("\n")
    val paragraphs = lines.mapIndexed { index, line ->
        if (index == 0) {
            // First segment continues the original paragraph: keep every attribute.
            paragraph.copy(text = line)
        } else {
            // New paragraphs from Enter inherit visual formatting, but:
            //  - must get a FRESH runtime id (they are a new identity, not the source)
            //  - a freshly split todo must not carry the "completed" state
            //  - title/subtitle/heading downgrade to body (Apple Notes behavior)
            //  - splitting an EMPTY todo/list row must NOT spawn another marker row,
            //    otherwise pressing Enter on a blank checkbox breeds empty checkboxes
            val sourceHasContent = paragraph.text.isNotBlank()
            paragraph.copy(
                id = java.util.UUID.randomUUID().toString(),
                text = line,
                completed = false,
                todo = paragraph.todo && sourceHasContent,
                listStyle = if (sourceHasContent) paragraph.listStyle else ParagraphListStyle.NONE,
                style = downgradeStyleOnSplit(paragraph.style)
            )
        }
    }
    var remaining = cursorOffset.coerceAtLeast(0)
    var targetLine = 0
    for (i in lines.indices) {
        val lineLength = lines[i].length
        if (remaining <= lineLength) {
            targetLine = i
            break
        }
        remaining -= lineLength + 1
        targetLine = i + 1
    }
    targetLine = targetLine.coerceIn(0, lines.lastIndex)
    val cursorInLine = remaining.coerceIn(0, lines[targetLine].length)
    return ParagraphSplitResult(paragraphs, targetLine, cursorInLine)
}

/** Title/subtitle/heading paragraphs drop to body once the user starts a new line. */
internal fun downgradeStyleOnSplit(style: ParagraphStyle): ParagraphStyle = when (style) {
    ParagraphStyle.TITLE, ParagraphStyle.SUBTITLE, ParagraphStyle.HEADING -> ParagraphStyle.BODY
    ParagraphStyle.BODY, ParagraphStyle.NOTE -> style
}

internal fun applyParagraphBoundaryDeletion(
    paragraphs: List<NoteParagraph>,
    index: Int,
    deleteBackward: Boolean
): ParagraphBoundaryChange? {
    if (index !in paragraphs.indices) return null
    val targetIndex = if (deleteBackward) index - 1 else index + 1
    if (targetIndex !in paragraphs.indices) return null
    val firstIndex = minOf(index, targetIndex)
    val secondIndex = maxOf(index, targetIndex)
    val first = paragraphs[firstIndex]
    val second = paragraphs[secondIndex]
    val joinedText = first.text + second.text
    val updated = paragraphs.toMutableList().apply {
        this[firstIndex] = first.copy(text = joinedText)
        removeAt(secondIndex)
    }
    return ParagraphBoundaryChange(
        paragraphs = updated,
        focusedParagraph = firstIndex,
        cursorOffset = first.text.length
    )
}

internal fun paragraphTextStyleFrom(
    baseStyle: TextStyle,
    paragraph: NoteParagraph,
    color: Color,
    fontSizeScale: Float,
    decorations: List<TextDecoration>
): TextStyle = baseStyle.copy(
    color = color,
    fontSize = baseStyle.fontSize * fontSizeScale,
    lineHeight = baseStyle.lineHeight * fontSizeScale,
    // Explicit Normal (never Unspecified) is required so the font engine always
    // re-resolves the face. Leaving weight unspecified was the root cause of
    // "italic only works together with bold".
    fontWeight = if (paragraph.bold) FontWeight.Bold else FontWeight.Normal,
    fontStyle = if (paragraph.italic) FontStyle.Italic else FontStyle.Normal,
    fontSynthesis = FontSynthesis.All,
    // Force a concrete family so synthesis / face selection is deterministic
    // across devices (system default can be quirky when weight/style change).
    fontFamily = baseStyle.fontFamily ?: FontFamily.Default,
    // Geometric skew is the reliable fallback when the platform does not
    // synthesize (or pick) an italic face — common for standalone italic,
    // lower API levels, and CJK. Applied at text-shaping time so caret and
    // selection stay consistent with the glyphs (unlike a graphicsLayer /
    // canvas skew on the whole BasicTextField).
    textGeometricTransform = if (paragraph.italic) {
        TextGeometricTransform(skewX = -0.2f)
    } else {
        null
    },
    textAlign = when (paragraph.alignment) {
        ParagraphAlignment.LEFT -> TextAlign.Start
        ParagraphAlignment.CENTER -> TextAlign.Center
        ParagraphAlignment.RIGHT -> TextAlign.End
    },
    textDecoration = if (decorations.isEmpty()) TextDecoration.None else TextDecoration.combine(decorations),
    // Do not trim the first/last line box: an empty or freshly split line would otherwise
    // clip the caret, which reads as "the cursor is only half visible".
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

@Composable
private fun ParagraphEditor(
    paragraph: NoteParagraph,
    showPlaceholder: Boolean,
    listNumber: Int,
    focusRequester: FocusRequester,
    requestedFocus: ParagraphFocusRequest?,
    onFocusRequestHandled: () -> Unit,
    onFocused: () -> Unit,
    onCheckedChange: () -> Unit,
    onTextChange: (text: String, cursorOffset: Int) -> Unit,
    onBoundaryDelete: (deleteBackward: Boolean) -> Boolean
) {
    // SINGLE SOURCE OF TRUTH for text: the parent's paragraph.text always wins.
    // Only the caret/selection (and IME composition) live locally.
    //
    // This is DERIVED on every recomposition instead of synced by LaunchedEffect(paragraph.text).
    // The old key-based sync silently failed in the most common case: pressing Enter at the END
    // of a paragraph leaves the parent text unchanged ("abc" -> "abc"), so the effect never
    // re-ran and the field kept its local "abc\n" — a phantom extra line with the caret
    // stranded in it, on top of the real new paragraph. That is the "Enter jumps two lines"
    // and "caret half drawn" bug.
    var localValue by remember { mutableStateOf(paragraphFieldValue(paragraph.text)) }
    val fieldValue = if (localValue.text == paragraph.text) {
        localValue
    } else {
        paragraphFieldValue(
            paragraph.text,
            localValue.selection.start.coerceIn(0, paragraph.text.length)
        )
    }
    // Keyed on the request token so repeated requests to the same offset still apply.
    LaunchedEffect(requestedFocus?.token) {
        val request = requestedFocus ?: return@LaunchedEffect
        localValue = paragraphFieldValue(
            paragraph.text,
            request.cursorOffset.coerceIn(0, paragraph.text.length)
        )
        focusRequester.requestFocus()
        onFocusRequestHandled()
    }
    val baseStyle = paragraphTextStyle(paragraph.style)
    val decorations = buildList {
        if (paragraph.completed || paragraph.strikethrough) add(TextDecoration.LineThrough)
        if (paragraph.underline) add(TextDecoration.Underline)
    }
    val textStyle = paragraphTextStyleFrom(
        baseStyle = baseStyle,
        paragraph = paragraph,
        color = paragraphTextColor(paragraph),
        fontSizeScale = paragraphFontScale(paragraph.fontSize),
        decorations = decorations
    )
    Row(
        Modifier.fillMaxWidth()
            .padding(start = (paragraph.indent * 20).dp, top = 3.dp, bottom = 3.dp)
            .padding(end = if (paragraph.italic) 8.dp else 0.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (paragraph.highlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .34f) else Color.Transparent)
            .padding(horizontal = if (paragraph.highlighted) 6.dp else 0.dp, vertical = if (paragraph.highlighted) 3.dp else 0.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (paragraph.todo) {
            Icon(
                if (paragraph.completed) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                if (paragraph.completed) "Mark todo incomplete" else "Mark todo complete",
                Modifier.size(28.dp).quietClickable(onClick = onCheckedChange).padding(3.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(6.dp))
        } else if (paragraph.listStyle != ParagraphListStyle.NONE) {
            Text(
                if (paragraph.listStyle == ParagraphListStyle.BULLET) "•" else "$listNumber.",
                Modifier.padding(top = 1.dp),
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(8.dp))
        } else if (paragraph.style == ParagraphStyle.NOTE) {
            Text(
                "|",
                Modifier.padding(top = 1.dp),
                style = textStyle.copy(fontWeight = FontWeight.Thin),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
            )
            Spacer(Modifier.size(6.dp))
        }
        BasicTextField(
            value = fieldValue,
            onValueChange = { value ->
                // Text goes to the parent (the single source of truth); the caret and the IME
                // composition stay here. Preserve composition so CJK candidate input is never
                // interrupted, and keep the user's real selection instead of collapsing it.
                localValue = value
                onTextChange(value.text, value.selection.start.coerceIn(0, value.text.length))
            },
            modifier = Modifier.fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocused() }
                .onPreviewKeyEvent { event ->
                    // Cross-paragraph merge is the ONLY path here (no zero-width marker).
                    if (event.type != KeyEventType.KeyDown || !fieldValue.selection.collapsed) {
                        return@onPreviewKeyEvent false
                    }
                    when {
                        event.key == Key.Backspace && fieldValue.selection.start == 0 -> onBoundaryDelete(true)
                        event.key == Key.Delete && fieldValue.selection.end == fieldValue.text.length -> onBoundaryDelete(false)
                        else -> false
                    }
                },
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            decorationBox = { field ->
                Box {
                    if (showPlaceholder && paragraph.text.isEmpty()) Text("Start writing", style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f))
                    field()
                }
            }
        )
    }
}

private fun numberedListOrdinal(paragraphs: List<NoteParagraph>, index: Int): Int {
    if (paragraphs[index].listStyle != ParagraphListStyle.NUMBERED) return 1
    var start = index
    while (start > 0 && paragraphs[start - 1].listStyle == ParagraphListStyle.NUMBERED) start--
    return index - start + 1
}

@Composable
private fun paragraphTextColor(paragraph: NoteParagraph): Color = when (paragraph.textColor) {
    ParagraphTextColor.DEFAULT -> if (paragraph.style == ParagraphStyle.NOTE) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    ParagraphTextColor.RED -> Color(0xFFB64A4A)
    ParagraphTextColor.ORANGE -> Color(0xFFB56A2D)
    ParagraphTextColor.YELLOW -> Color(0xFF947516)
    ParagraphTextColor.GREEN -> Color(0xFF4E7D59)
    ParagraphTextColor.BLUE -> Color(0xFF436F9D)
    ParagraphTextColor.PURPLE -> Color(0xFF75608E)
    ParagraphTextColor.GRAY -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun paragraphFontScale(fontSize: ParagraphFontSize): Float = when (fontSize) {
    ParagraphFontSize.SMALL -> .88f
    ParagraphFontSize.NORMAL -> 1f
    ParagraphFontSize.LARGE -> 1.16f
    ParagraphFontSize.EXTRA_LARGE -> 1.34f
}

private fun nextFontSize(fontSize: ParagraphFontSize): ParagraphFontSize = when (fontSize) {
    ParagraphFontSize.SMALL -> ParagraphFontSize.NORMAL
    ParagraphFontSize.NORMAL -> ParagraphFontSize.LARGE
    ParagraphFontSize.LARGE -> ParagraphFontSize.EXTRA_LARGE
    ParagraphFontSize.EXTRA_LARGE -> ParagraphFontSize.EXTRA_LARGE
}

private fun previousFontSize(fontSize: ParagraphFontSize): ParagraphFontSize = when (fontSize) {
    ParagraphFontSize.SMALL -> ParagraphFontSize.SMALL
    ParagraphFontSize.NORMAL -> ParagraphFontSize.SMALL
    ParagraphFontSize.LARGE -> ParagraphFontSize.NORMAL
    ParagraphFontSize.EXTRA_LARGE -> ParagraphFontSize.LARGE
}

@Composable
private fun paragraphTextStyle(style: ParagraphStyle): TextStyle = when (style) {
    ParagraphStyle.TITLE -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 36.sp)
    ParagraphStyle.SUBTITLE -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 30.sp)
    ParagraphStyle.HEADING -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 25.sp)
    ParagraphStyle.BODY -> MaterialTheme.typography.bodyLarge
    ParagraphStyle.NOTE -> MaterialTheme.typography.bodyMedium
}

@Composable
private fun FormatPanel(paragraph: NoteParagraph, onUpdate: ((NoteParagraph) -> NoteParagraph) -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f))
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .72f))
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ParagraphStyle.entries.forEach { style ->
                val label = when (style) {
                    ParagraphStyle.TITLE -> "Title"
                    ParagraphStyle.SUBTITLE -> "Subtitle"
                    ParagraphStyle.HEADING -> "Heading"
                    ParagraphStyle.BODY -> "Body"
                    ParagraphStyle.NOTE -> "Note"
                }
                FormatTextButton(label, paragraph.style == style) { onUpdate { it.copy(style = style, italic = style == ParagraphStyle.NOTE || it.italic) } }
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .72f))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FormatTextButton("A-", false, paragraph.fontSize != ParagraphFontSize.SMALL) { onUpdate { it.copy(fontSize = previousFontSize(it.fontSize)) } }
            ParagraphFontSize.entries.forEach { size ->
                val label = when (size) {
                    ParagraphFontSize.SMALL -> "S"
                    ParagraphFontSize.NORMAL -> "M"
                    ParagraphFontSize.LARGE -> "L"
                    ParagraphFontSize.EXTRA_LARGE -> "XL"
                }
                FormatTextButton(label, paragraph.fontSize == size) { onUpdate { it.copy(fontSize = size) } }
            }
            FormatTextButton("A+", false, paragraph.fontSize != ParagraphFontSize.EXTRA_LARGE) { onUpdate { it.copy(fontSize = nextFontSize(it.fontSize)) } }
            FormatDivider()
            FormatIconButton(Icons.Default.FormatBold, "Bold", paragraph.bold) { onUpdate { it.copy(bold = !it.bold) } }
            FormatIconButton(Icons.Default.FormatItalic, "Italic", paragraph.italic) { onUpdate { it.copy(italic = !it.italic) } }
            FormatIconButton(Icons.Default.FormatUnderlined, "Underline", paragraph.underline) { onUpdate { it.copy(underline = !it.underline) } }
            FormatIconButton(Icons.Default.FormatStrikethrough, "Strikethrough", paragraph.completed || paragraph.strikethrough) {
                onUpdate { it.copy(strikethrough = !it.strikethrough, completed = if (it.completed) false else it.completed) }
            }
            FormatDivider()
            FormatIconButton(Icons.Default.FormatListBulleted, "Bullet list", paragraph.listStyle == ParagraphListStyle.BULLET) {
                onUpdate { it.copy(listStyle = if (it.listStyle == ParagraphListStyle.BULLET) ParagraphListStyle.NONE else ParagraphListStyle.BULLET, todo = false, completed = false) }
            }
            FormatIconButton(Icons.Default.FormatListNumbered, "Numbered list", paragraph.listStyle == ParagraphListStyle.NUMBERED) {
                onUpdate { it.copy(listStyle = if (it.listStyle == ParagraphListStyle.NUMBERED) ParagraphListStyle.NONE else ParagraphListStyle.NUMBERED, todo = false, completed = false) }
            }
            FormatDivider()
            FormatIconButton(Icons.AutoMirrored.Filled.FormatAlignLeft, "Align left", paragraph.alignment == ParagraphAlignment.LEFT) { onUpdate { it.copy(alignment = ParagraphAlignment.LEFT) } }
            FormatIconButton(Icons.Default.FormatAlignCenter, "Align center", paragraph.alignment == ParagraphAlignment.CENTER) { onUpdate { it.copy(alignment = ParagraphAlignment.CENTER) } }
            FormatIconButton(Icons.AutoMirrored.Filled.FormatAlignRight, "Align right", paragraph.alignment == ParagraphAlignment.RIGHT) { onUpdate { it.copy(alignment = ParagraphAlignment.RIGHT) } }
            FormatDivider()
            FormatIconButton(Icons.AutoMirrored.Filled.FormatIndentDecrease, "Decrease indent", false, paragraph.indent > 0) { onUpdate { it.copy(indent = (it.indent - 1).coerceAtLeast(0)) } }
            FormatIconButton(Icons.AutoMirrored.Filled.FormatIndentIncrease, "Increase indent", false, paragraph.indent < 4) { onUpdate { it.copy(indent = (it.indent + 1).coerceAtMost(4)) } }
            FormatIconButton(Icons.Default.Highlight, "Highlight", paragraph.highlighted) { onUpdate { it.copy(highlighted = !it.highlighted) } }
        }
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .72f))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FormatColorText, "Text color", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            ParagraphTextColor.entries.forEach { color ->
                ColorButton(color, paragraph.textColor == color) { onUpdate { it.copy(textColor = color) } }
            }
        }
    }
}

@Composable
private fun FormatTextButton(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label,
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .quietClickable(enabled = enabled, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
        color = when {
            selected -> MaterialTheme.colorScheme.onSurface
            enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .38f)
        }
    )
}

@Composable
private fun FormatIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .quietClickable(enabled = enabled, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FormatDivider() {
    Box(Modifier.padding(horizontal = 3.dp).size(width = 1.dp, height = 22.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun ColorButton(color: ParagraphTextColor, selected: Boolean, onClick: () -> Unit) {
    val swatch = when (color) {
        ParagraphTextColor.DEFAULT -> MaterialTheme.colorScheme.onSurface
        ParagraphTextColor.RED -> Color(0xFFB64A4A)
        ParagraphTextColor.ORANGE -> Color(0xFFB56A2D)
        ParagraphTextColor.YELLOW -> Color(0xFFB09222)
        ParagraphTextColor.GREEN -> Color(0xFF4E7D59)
        ParagraphTextColor.BLUE -> Color(0xFF436F9D)
        ParagraphTextColor.PURPLE -> Color(0xFF75608E)
        ParagraphTextColor.GRAY -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        Modifier.size(23.dp).clip(CircleShape)
            .background(swatch.copy(alpha = if (selected) 1f else .72f))
            .quietClickable(onClick = onClick)
            .semantics {
                contentDescription = "${color.name.lowercase()} text color"
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        if (selected) Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface))
    }
}
