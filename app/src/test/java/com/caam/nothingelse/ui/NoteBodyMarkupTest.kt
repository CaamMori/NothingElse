package com.caam.nothingelse.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBodyMarkupTest {
    @Test
    fun `italic toggle round-trips on NOTE paragraphs`() {
        val italicOn = NoteParagraph("Note text", style = ParagraphStyle.NOTE, italic = true)
        val italicOff = NoteParagraph("Note text", style = ParagraphStyle.NOTE, italic = false)

        assertEquals(italicOn, parseNoteBody(serializeNoteBody(listOf(italicOn))).single())
        assertEquals(italicOff, parseNoteBody(serializeNoteBody(listOf(italicOff))).single())
    }

    @Test
    fun `NOTE paragraph without explicit italic attribute defaults to italic`() {
        val body = "[[ne:style=note]]Legacy note"

        val parsed = parseNoteBody(body).single()

        assertEquals(ParagraphStyle.NOTE, parsed.style)
        assertTrue(parsed.italic)
    }

    @Test
    fun `italic attribute is serialized on non-NOTE paragraphs`() {
        val italicOn = NoteParagraph("Bold text", style = ParagraphStyle.BODY, italic = true)
        val italicOff = NoteParagraph("Plain text", style = ParagraphStyle.BODY, italic = false)

        assertTrue(serializeNoteBody(listOf(italicOn)).contains("italic"))
        assertFalse(serializeNoteBody(listOf(italicOff)).contains("italic"))
    }

    @Test
    fun `plain bodies remain unchanged`() {
        val body = "First paragraph\nSecond paragraph"

        assertEquals(body, serializeNoteBody(parseNoteBody(body)))
    }

    @Test
    fun `all paragraph styles survive serialization`() {
        val paragraphs = ParagraphStyle.entries.map { NoteParagraph(it.name, style = it) }

        assertEquals(paragraphs, parseNoteBody(serializeNoteBody(paragraphs)))
    }

    @Test
    fun `todo completion survives serialization`() {
        val paragraphs = listOf(
            NoteParagraph("Open", todo = true),
            NoteParagraph("Done", todo = true, completed = true)
        )

        assertEquals(paragraphs, parseNoteBody(serializeNoteBody(paragraphs)))
    }

    @Test
    fun `all paragraph formatting survives serialization`() {
        val paragraph = NoteParagraph(
            text = "Formatted",
            style = ParagraphStyle.HEADING,
            bold = true,
            italic = true,
            underline = true,
            strikethrough = true,
            listStyle = ParagraphListStyle.NUMBERED,
            alignment = ParagraphAlignment.RIGHT,
            indent = 3,
            highlighted = true,
            textColor = ParagraphTextColor.PURPLE,
            fontSize = ParagraphFontSize.EXTRA_LARGE
        )

        assertEquals(listOf(paragraph), parseNoteBody(serializeNoteBody(listOf(paragraph))))
    }

    @Test
    fun `empty formatted paragraphs retain their state`() {
        val paragraph = NoteParagraph("", bold = true, alignment = ParagraphAlignment.CENTER)

        assertEquals(listOf(paragraph), parseNoteBody(serializeNoteBody(listOf(paragraph))))
    }

    @Test
    fun `indent is kept within supported bounds`() {
        val stored = "[[ne:indent=99]]Indented"

        assertEquals(4, parseNoteBody(stored).single().indent)
    }

    @Test
    fun `visible body hides storage markers`() {
        val stored = "[[ne:style=title;bold;align=center;color=blue;size=large]]Heading\n[[ne:todo=done;highlight]]Finished"

        assertEquals("Heading\nFinished", visibleNoteBody(stored))
    }

    @Test
    fun `unknown markers remain visible as plain text`() {
        val body = "[[ne:future=value]]Keep this"

        assertEquals(body, visibleNoteBody(body))
    }

    @Test
    fun `deleted marker stores timestamp and stays hidden`() {
        val body = "[[ne:style=heading;bold]]Heading\nBody"
        val stored = markNoteBodyDeleted(body, 1_725_000_000_000L)

        assertTrue(isDeletedNoteBody(stored))
        assertEquals(1_725_000_000_000L, deletedAtFromNoteBody(stored))
        assertEquals("Heading\nBody", visibleNoteBody(stored))
    }

    @Test
    fun `marking an already deleted body replaces its timestamp`() {
        val once = markNoteBodyDeleted("Body", 100L)
        val twice = markNoteBodyDeleted(once, 200L)

        assertEquals(200L, deletedAtFromNoteBody(twice))
        assertEquals("Body", restoreDeletedNoteBody(twice))
    }

    @Test
    fun `restoring removes only a valid first line deleted marker`() {
        val body = "First\n[[ne:deletedAt=100]]Visible second line"

        assertFalse(isDeletedNoteBody(body))
        assertNull(deletedAtFromNoteBody(body))
        assertEquals(body, restoreDeletedNoteBody(body))
        assertEquals(body, visibleNoteBody(body))
    }

    @Test
    fun `invalid deleted timestamp still marks body deleted and stays hidden`() {
        val body = "[[ne:deletedAt=invalid]]\nKeep this"

        assertTrue(isDeletedNoteBody(body))
        assertNull(deletedAtFromNoteBody(body))
        assertEquals("Keep this", visibleNoteBody(body))
        assertEquals("Keep this", restoreDeletedNoteBody(body))
    }

    @Test
    fun `deleted note expires only after thirty full days`() {
        val deletedAt = 1_725_000_000_000L
        val body = markNoteBodyDeleted("Body", deletedAt)

        assertFalse(isDeletedNoteExpired(body, deletedAt + DELETED_NOTE_RETENTION_MS))
        assertTrue(isDeletedNoteExpired(body, deletedAt + DELETED_NOTE_RETENTION_MS + 1L))
    }

    @Test
    fun `deleted note with invalid timestamp does not auto expire`() {
        assertFalse(isDeletedNoteExpired("[[ne:deletedAt=invalid]]\nBody", Long.MAX_VALUE))
        assertFalse(isDeletedNoteExpired("Body", Long.MAX_VALUE))
    }

    @Test
    fun `notebook marker stores notebook and stays hidden`() {
        val stored = setNoteBodyNotebook("[[ne:style=heading]]Heading\nBody", "Work")

        assertEquals("Work", notebookFromNoteBody(stored))
        assertEquals("Heading\nBody", visibleNoteBody(stored))
        assertEquals(listOf(NoteParagraph("Heading", style = ParagraphStyle.HEADING), NoteParagraph("Body")), parseNoteBody(stored))
    }

    @Test
    fun `default notebook removes notebook marker`() {
        val stored = setNoteBodyNotebook("Body", "Work")

        assertEquals("Body", setNoteBodyNotebook(stored, DEFAULT_NOTEBOOK_NAME))
        assertEquals(DEFAULT_NOTEBOOK_NAME, notebookFromNoteBody("Body"))
    }

    @Test
    fun `moving to default notebook preserves deleted metadata and visible content`() {
        val stored = markNoteBodyDeleted(setNoteBodyNotebook("Body", "Work"), 275L)
        val moved = setNoteBodyNotebook(stored, DEFAULT_NOTEBOOK_NAME)

        assertEquals(DEFAULT_NOTEBOOK_NAME, notebookFromNoteBody(moved))
        assertEquals(275L, deletedAtFromNoteBody(moved))
        assertEquals("Body", visibleNoteBody(moved))
    }

    @Test
    fun `deleted and notebook markers coexist independently`() {
        val notebookBody = setNoteBodyNotebook("Body", "Personal")
        val deletedBody = markNoteBodyDeleted(notebookBody, 300L)

        assertTrue(isDeletedNoteBody(deletedBody))
        assertEquals(300L, deletedAtFromNoteBody(deletedBody))
        assertEquals("Personal", notebookFromNoteBody(deletedBody))
        assertEquals("Body", visibleNoteBody(deletedBody))
        assertEquals("Personal", notebookFromNoteBody(restoreDeletedNoteBody(deletedBody)))
        assertFalse(isDeletedNoteBody(restoreDeletedNoteBody(deletedBody)))
    }

    @Test
    fun `renaming notebook updates matching marker and preserves deleted metadata`() {
        val stored = markNoteBodyDeleted(setNoteBodyNotebook("Body", "Work"), 350L)
        val renamed = renameNoteBodyNotebook(stored, "Work", "Projects")

        assertEquals("Projects", notebookFromNoteBody(renamed))
        assertEquals(350L, deletedAtFromNoteBody(renamed))
        assertEquals("Body", visibleNoteBody(renamed))
    }

    @Test
    fun `renaming notebook leaves other notebooks unchanged`() {
        val stored = setNoteBodyNotebook("Body", "Personal")

        assertEquals(stored, renameNoteBodyNotebook(stored, "Work", "Projects"))
    }

    @Test
    fun `default notebook cannot be renamed`() {
        assertEquals("Body", renameNoteBodyNotebook("Body", DEFAULT_NOTEBOOK_NAME, "Projects"))
    }

    @Test
    fun `replacing note content preserves internal metadata`() {
        val stored = markNoteBodyDeleted(setNoteBodyNotebook("Old", "Archive"), 400L)
        val replaced = replaceNoteBodyContent(stored, "[[ne:bold]]New")

        assertTrue(isDeletedNoteBody(replaced))
        assertEquals("Archive", notebookFromNoteBody(replaced))
        assertEquals("New", visibleNoteBody(replaced))
        assertEquals(listOf(NoteParagraph("New", bold = true)), parseNoteBody(replaced))
    }

    @Test
    fun `plain body text maps to paragraphs while keeping existing formatting by line`() {
        val previous = listOf(
            NoteParagraph("Old first", bold = true),
            NoteParagraph("Old second", italic = true)
        )

        assertEquals(
            listOf(
                previous[0].copy(text = "New first"),
                previous[1].copy(text = "New second")
            ),
            paragraphsFromPlainText("New first\nNew second", previous)
        )
    }

    @Test
    fun `plain body text resets formatting when adding empty lines`() {
        val previous = listOf(NoteParagraph("First", bold = true))

        assertEquals(
            listOf(
                NoteParagraph("First"),
                NoteParagraph("")
            ),
            paragraphsFromPlainText("First\n", previous)
        )
    }

    @Test
    fun `plain body text resets formatting when deleting an empty line`() {
        val previous = listOf(NoteParagraph("First", bold = true), NoteParagraph(""))

        assertEquals(
            listOf(NoteParagraph("First")),
            paragraphsFromPlainText("First", previous)
        )
    }

    @Test
    fun `plain body text keeps one empty paragraph for empty notes`() {
        assertEquals(listOf(NoteParagraph("")), paragraphsFromPlainText("", emptyList()))
    }
}
