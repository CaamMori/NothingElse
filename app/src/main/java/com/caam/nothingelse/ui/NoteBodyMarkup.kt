package com.caam.nothingelse.ui

internal enum class ParagraphStyle {
    TITLE,
    SUBTITLE,
    HEADING,
    BODY,
    NOTE
}

internal enum class ParagraphListStyle { NONE, BULLET, NUMBERED }

internal enum class ParagraphAlignment { LEFT, CENTER, RIGHT }

internal enum class ParagraphTextColor { DEFAULT, RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, GRAY }

internal enum class ParagraphFontSize { SMALL, NORMAL, LARGE, EXTRA_LARGE }

internal data class NoteParagraph(
    val text: String,
    // Runtime-only stable identity. NEVER serialized to [[ne:...]] markers or disk.
    // copy() preserves it across per-keystroke edits regardless of position.
    val id: String = java.util.UUID.randomUUID().toString(),
    val style: ParagraphStyle = ParagraphStyle.BODY,
    val todo: Boolean = false,
    val completed: Boolean = false,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val listStyle: ParagraphListStyle = ParagraphListStyle.NONE,
    val alignment: ParagraphAlignment = ParagraphAlignment.LEFT,
    val indent: Int = 0,
    val highlighted: Boolean = false,
    val textColor: ParagraphTextColor = ParagraphTextColor.DEFAULT,
    val fontSize: ParagraphFontSize = ParagraphFontSize.NORMAL
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoteParagraph) return false
        return text == other.text &&
            style == other.style &&
            todo == other.todo &&
            completed == other.completed &&
            bold == other.bold &&
            italic == other.italic &&
            underline == other.underline &&
            strikethrough == other.strikethrough &&
            listStyle == other.listStyle &&
            alignment == other.alignment &&
            indent == other.indent &&
            highlighted == other.highlighted &&
            textColor == other.textColor &&
            fontSize == other.fontSize
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + todo.hashCode()
        result = 31 * result + completed.hashCode()
        result = 31 * result + bold.hashCode()
        result = 31 * result + italic.hashCode()
        result = 31 * result + underline.hashCode()
        result = 31 * result + strikethrough.hashCode()
        result = 31 * result + listStyle.hashCode()
        result = 31 * result + alignment.hashCode()
        result = 31 * result + indent
        result = 31 * result + highlighted.hashCode()
        result = 31 * result + textColor.hashCode()
        result = 31 * result + fontSize.hashCode()
        return result
    }
}

private const val MARKER_START = "[[ne:"
private const val MARKER_END = "]]"
private const val DELETED_MARKER_START = "[[ne:deletedAt="
private const val NOTEBOOK_MARKER_START = "[[ne:notebook="
internal const val DEFAULT_NOTEBOOK_NAME = "Default Notebook"
internal const val DELETED_NOTE_RETENTION_DAYS = 30
internal const val DELETED_NOTE_RETENTION_MS = DELETED_NOTE_RETENTION_DAYS * 24L * 60L * 60L * 1000L

private fun isDeletedMarkerLine(line: String): Boolean =
    line.startsWith(DELETED_MARKER_START) && line.endsWith(MARKER_END)

private fun isNotebookMarkerLine(line: String): Boolean =
    line.startsWith(NOTEBOOK_MARKER_START) && line.endsWith(MARKER_END)

private fun splitInternalMetadata(body: String): Pair<List<String>, String> {
    if (body.isEmpty()) return emptyList<String>() to ""
    val lines = body.split("\n")
    val metadata = mutableListOf<String>()
    var index = 0
    while (index < lines.size && (isDeletedMarkerLine(lines[index]) || isNotebookMarkerLine(lines[index]))) {
        metadata += lines[index]
        index++
    }
    return metadata to lines.drop(index).joinToString("\n")
}

private fun sanitizeNotebookName(notebook: String): String = notebook
    .replace('\n', ' ')
    .replace('\r', ' ')
    .replace(MARKER_END, "")
    .trim()
    .ifBlank { DEFAULT_NOTEBOOK_NAME }

private fun joinInternalMetadata(metadata: List<String>, content: String): String =
    (metadata + listOf(content).filter(String::isNotEmpty)).joinToString("\n")

internal fun isDeletedNoteBody(body: String): Boolean = splitInternalMetadata(body).first.any(::isDeletedMarkerLine)

internal fun deletedAtFromNoteBody(body: String): Long? = splitInternalMetadata(body).first
    .firstOrNull(::isDeletedMarkerLine)
    ?.let { line -> line.substring(DELETED_MARKER_START.length, line.length - MARKER_END.length).toLongOrNull() }

internal fun isDeletedNoteExpired(body: String, now: Long): Boolean =
    deletedAtFromNoteBody(body)?.let { deletedAt -> now - deletedAt > DELETED_NOTE_RETENTION_MS } == true

internal fun markNoteBodyDeleted(body: String, deletedAt: Long): String {
    val (metadata, content) = splitInternalMetadata(body)
    val kept = metadata.filterNot(::isDeletedMarkerLine)
    return joinInternalMetadata(listOf("$DELETED_MARKER_START$deletedAt$MARKER_END") + kept, content)
}

internal fun restoreDeletedNoteBody(body: String): String {
    val (metadata, content) = splitInternalMetadata(body)
    return joinInternalMetadata(metadata.filterNot(::isDeletedMarkerLine), content)
}

// TECHDEBT: notebook 归属从 body 字符串解析，未来应迁移至独立 Room 字段
internal fun notebookFromNoteBody(body: String): String = splitInternalMetadata(body).first
    .firstOrNull(::isNotebookMarkerLine)
    ?.let { line -> line.substring(NOTEBOOK_MARKER_START.length, line.length - MARKER_END.length).trim() }
    ?.ifBlank { DEFAULT_NOTEBOOK_NAME }
    ?: DEFAULT_NOTEBOOK_NAME

// TECHDEBT: notebook 归属从 body 字符串解析，未来应迁移至独立 Room 字段
internal fun setNoteBodyNotebook(body: String, notebook: String): String {
    val name = sanitizeNotebookName(notebook)
    val (metadata, content) = splitInternalMetadata(body)
    val kept = metadata.filterNot(::isNotebookMarkerLine)
    val updated = if (name == DEFAULT_NOTEBOOK_NAME) kept else kept + "$NOTEBOOK_MARKER_START$name$MARKER_END"
    return joinInternalMetadata(updated, content)
}

// TECHDEBT: notebook 归属从 body 字符串解析，未来应迁移至独立 Room 字段
internal fun renameNoteBodyNotebook(body: String, oldNotebook: String, newNotebook: String): String {
    val oldName = sanitizeNotebookName(oldNotebook)
    if (oldName == DEFAULT_NOTEBOOK_NAME) return body
    return if (notebookFromNoteBody(body) == oldName) setNoteBodyNotebook(body, newNotebook) else body
}

private fun bodyWithoutInternalMetadata(body: String): String = splitInternalMetadata(body).second

internal fun replaceNoteBodyContent(body: String, content: String): String {
    val metadata = splitInternalMetadata(body).first
    return joinInternalMetadata(metadata, content)
}

internal fun parseNoteBody(body: String): List<NoteParagraph> {
    val content = bodyWithoutInternalMetadata(body)
    if (content.isEmpty()) return listOf(NoteParagraph(""))
    return content.split("\n").map(::parseParagraph)
}

private fun parseParagraph(line: String): NoteParagraph {
    if (!line.startsWith(MARKER_START)) return NoteParagraph(line)
    val markerEnd = line.indexOf(MARKER_END, MARKER_START.length)
    if (markerEnd < 0) return NoteParagraph(line)

    var style = ParagraphStyle.BODY
    var todo = false
    var completed = false
    var bold = false
    var italic = false
    var italicExplicit = false
    var underline = false
    var strikethrough = false
    var listStyle = ParagraphListStyle.NONE
    var alignment = ParagraphAlignment.LEFT
    var indent = 0
    var highlighted = false
    var textColor = ParagraphTextColor.DEFAULT
    var fontSize = ParagraphFontSize.NORMAL
    val attributes = line.substring(MARKER_START.length, markerEnd).split(";")
    if (attributes.any { attribute ->
            attribute !in setOf("todo=open", "todo=done", "bold", "italic", "italic=false", "underline", "strike", "highlight") &&
                !attribute.startsWith("style=") &&
                !attribute.startsWith("list=") &&
                !attribute.startsWith("align=") &&
                !attribute.startsWith("indent=") &&
                !attribute.startsWith("color=") &&
                !attribute.startsWith("size=")
        }
    ) return NoteParagraph(line)
    attributes.forEach { attribute ->
        when {
            attribute.startsWith("style=") -> {
                style = runCatching {
                    ParagraphStyle.valueOf(attribute.substringAfter("style=").uppercase())
                }.getOrElse { return NoteParagraph(line) }
            }
            attribute == "todo=open" -> todo = true
            attribute == "todo=done" -> {
                todo = true
                completed = true
            }
            attribute == "bold" -> bold = true
            attribute == "italic" -> { italic = true; italicExplicit = true }
            attribute == "italic=false" -> { italic = false; italicExplicit = true }
            attribute == "underline" -> underline = true
            attribute == "strike" -> strikethrough = true
            attribute == "highlight" -> highlighted = true
            attribute.startsWith("list=") -> {
                listStyle = parseEnumAttribute<ParagraphListStyle>(attribute, "list") ?: return NoteParagraph(line)
            }
            attribute.startsWith("align=") -> {
                alignment = parseEnumAttribute<ParagraphAlignment>(attribute, "align") ?: return NoteParagraph(line)
            }
            attribute.startsWith("indent=") -> {
                indent = attribute.substringAfter("indent=").toIntOrNull()?.coerceIn(0, 4) ?: return NoteParagraph(line)
            }
            attribute.startsWith("color=") -> {
                textColor = parseEnumAttribute<ParagraphTextColor>(attribute, "color") ?: return NoteParagraph(line)
            }
            attribute.startsWith("size=") -> {
                fontSize = parseEnumAttribute<ParagraphFontSize>(attribute, "size") ?: return NoteParagraph(line)
            }
        }
    }
    return NoteParagraph(
        text = line.substring(markerEnd + MARKER_END.length),
        style = style,
        todo = todo,
        completed = completed,
        bold = bold,
        italic = if (!italicExplicit && style == ParagraphStyle.NOTE) true else italic,
        underline = underline,
        strikethrough = strikethrough,
        listStyle = listStyle,
        alignment = alignment,
        indent = indent,
        highlighted = highlighted,
        textColor = textColor,
        fontSize = fontSize
    )
}

private inline fun <reified T : Enum<T>> parseEnumAttribute(attribute: String, name: String): T? =
    runCatching { enumValueOf<T>(attribute.substringAfter("$name=").uppercase()) }.getOrNull()

internal fun serializeNoteBody(paragraphs: List<NoteParagraph>): String = paragraphs.joinToString("\n") { paragraph ->
    val attributes = buildList {
        if (paragraph.style != ParagraphStyle.BODY) add("style=${paragraph.style.name.lowercase()}")
        if (paragraph.todo) add(if (paragraph.completed) "todo=done" else "todo=open")
        if (paragraph.bold) add("bold")
        if (paragraph.italic || paragraph.style == ParagraphStyle.NOTE) add(if (paragraph.italic) "italic" else "italic=false")
        if (paragraph.underline) add("underline")
        if (paragraph.strikethrough) add("strike")
        if (paragraph.listStyle != ParagraphListStyle.NONE) add("list=${paragraph.listStyle.name.lowercase()}")
        if (paragraph.alignment != ParagraphAlignment.LEFT) add("align=${paragraph.alignment.name.lowercase()}")
        if (paragraph.indent > 0) add("indent=${paragraph.indent.coerceIn(0, 4)}")
        if (paragraph.highlighted) add("highlight")
        if (paragraph.textColor != ParagraphTextColor.DEFAULT) add("color=${paragraph.textColor.name.lowercase()}")
        if (paragraph.fontSize != ParagraphFontSize.NORMAL) add("size=${paragraph.fontSize.name.lowercase()}")
    }
    if (attributes.isEmpty()) paragraph.text else "$MARKER_START${attributes.joinToString(";")}$MARKER_END${paragraph.text}"
}

internal fun visibleNoteBody(body: String): String =
    parseNoteBody(bodyWithoutInternalMetadata(body)).joinToString("\n", transform = NoteParagraph::text)

internal fun paragraphsFromPlainText(text: String, previous: List<NoteParagraph>): List<NoteParagraph> {
    val lines = text.split("\n")
    if (lines.size != previous.size) return lines.map(::NoteParagraph)
    return lines.mapIndexed { index, line ->
        previous[index].copy(text = line)
    }
}
