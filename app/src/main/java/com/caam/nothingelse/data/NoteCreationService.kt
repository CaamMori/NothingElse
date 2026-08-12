package com.caam.nothingelse.data

data class PreparedNote(val title: String, val content: String)

class BlankImportException : IllegalArgumentException("Import content cannot be blank")

class NoteCreationService {
    fun prepareImport(rawContent: String): PreparedNote {
        if (rawContent.isBlank()) throw BlankImportException()
        val title = extractTitle(rawContent)
        val firstLine = rawContent.lineSequence().firstOrNull().orEmpty()
        val rest = rawContent.substringAfter("\n", "")
        // Only drop the first line from the body when it was fully consumed as the title.
        // extractTitle() truncates a long plain first line to 30 chars, so dropping it
        // blindly would silently lose the remainder of that line.
        val body = if (firstLineFullyBecomesTitle(firstLine, title)) rest.trimStart('\n') else rawContent
        return PreparedNote(title, body)
    }

    private fun firstLineFullyBecomesTitle(firstLine: String, title: String): Boolean {
        val visibleFirstLine = firstLine
            .replace(Regex("^\\[\\[ne:[^]]+]]"), "")
            .removePrefix("# ")
            .trim()
        return title.isNotEmpty() && visibleFirstLine == title
    }

    internal fun extractTitle(content: String): String {
        val firstLine = content.lineSequence().firstOrNull().orEmpty()
        val markdownTitle = firstLine
            .takeIf { it.startsWith("# ") }
            ?.removePrefix("# ")
            ?.trim()
            .orEmpty()
        val nativeTitle = firstLine
            .takeIf { it.startsWith("[[ne:style=title]]") }
            ?.removePrefix("[[ne:style=title]]")
            ?.trim()
            .orEmpty()
        val visibleFirstLine = firstLine.replace(Regex("^\\[\\[ne:[^]]+]]"), "").trim()
        return markdownTitle.ifBlank { nativeTitle }.ifBlank { visibleFirstLine.take(30) }.ifBlank { "Untitled" }
    }
}
