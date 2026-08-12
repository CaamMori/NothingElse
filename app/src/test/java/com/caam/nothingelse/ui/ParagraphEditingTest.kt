package com.caam.nothingelse.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp
import org.junit.Test

class ParagraphEditingTest {
    @Test
    fun `splitting plain text creates one paragraph per line`() {
        val paragraph = NoteParagraph("FirstSecond")

        assertEquals(
            listOf(paragraph.copy(text = "First"), paragraph.copy(text = "Second")),
            applyParagraphTextChange(paragraph, "First\nSecond", cursorOffset = 6).paragraphs
        )
    }

    @Test
    fun `first split paragraph keeps original id and new lines get fresh ids`() {
        val paragraph = NoteParagraph("FirstSecondThird")
        val result = applyParagraphTextChange(paragraph, "First\nSecond\nThird", cursorOffset = 6).paragraphs

        // Continuation keeps identity; split-off paragraphs are new identities.
        assertEquals(paragraph.id, result[0].id)
        assertNotEquals(paragraph.id, result[1].id)
        assertNotEquals(paragraph.id, result[2].id)
        assertNotEquals(result[1].id, result[2].id)
        assertTrue(result.all { it.id.isNotEmpty() })
    }

    @Test
    fun `splitting todo paragraph inherits todo but resets completed`() {
        val paragraph = NoteParagraph("Buy milk", todo = true, completed = true)

        assertEquals(
            listOf(
                paragraph.copy(text = "Buy"),
                paragraph.copy(text = "milk", completed = false)
            ),
            applyParagraphTextChange(paragraph, "Buy\nmilk", cursorOffset = 4).paragraphs
        )
    }

    @Test
    fun `splitting bullet and numbered paragraphs inherits list state`() {
        listOf(ParagraphListStyle.BULLET, ParagraphListStyle.NUMBERED).forEach { listStyle ->
            val paragraph = NoteParagraph("OneTwo", listStyle = listStyle, indent = 2)

            assertEquals(
                listOf(paragraph.copy(text = "One"), paragraph.copy(text = "Two")),
                applyParagraphTextChange(paragraph, "One\nTwo", cursorOffset = 4).paragraphs
            )
        }
    }

    @Test
    fun `splitting formatted paragraph inherits visual attributes and downgrades heading`() {
        val paragraph = NoteParagraph(
            text = "HeadingDetails",
            style = ParagraphStyle.HEADING,
            bold = true,
            italic = true,
            underline = true,
            strikethrough = true,
            alignment = ParagraphAlignment.RIGHT,
            indent = 3,
            highlighted = true,
            textColor = ParagraphTextColor.PURPLE,
            fontSize = ParagraphFontSize.EXTRA_LARGE
        )

        assertEquals(
            listOf(
                paragraph.copy(text = "Heading"),
                paragraph.copy(text = "Details", style = ParagraphStyle.BODY)
            ),
            applyParagraphTextChange(paragraph, "Heading\nDetails", cursorOffset = 8).paragraphs
        )
    }

    @Test
    fun `splitting body paragraph keeps its non-title style`() {
        val paragraph = NoteParagraph("OneTwo", style = ParagraphStyle.NOTE, fontSize = ParagraphFontSize.LARGE)

        assertEquals(
            listOf(
                paragraph.copy(text = "One"),
                paragraph.copy(text = "Two")
            ),
            applyParagraphTextChange(paragraph, "One\nTwo", cursorOffset = 4).paragraphs
        )
    }

    @Test
    fun `enter at line end lands cursor at start of new empty paragraph`() {
        val paragraph = NoteParagraph("ABC")

        // "ABC" + Enter at end -> "ABC" / "" with cursor at offset 0 of new line.
        val result = applyParagraphTextChange(paragraph, "ABC\n", cursorOffset = 4)

        assertEquals(listOf(paragraph.copy(text = "ABC"), NoteParagraph("")), result.paragraphs)
        assertEquals(1, result.cursorParagraphOffset)
        assertEquals(0, result.cursorTextOffset)
    }

    @Test
    fun `enter in middle lands cursor at start of second paragraph`() {
        val paragraph = NoteParagraph("ABCDEF")

        // Cursor between C and D (offset 3), Enter -> "ABC" / "DEF", cursor at D (offset 0).
        val result = applyParagraphTextChange(paragraph, "ABC\nDEF", cursorOffset = 4)

        assertEquals(listOf(paragraph.copy(text = "ABC"), NoteParagraph("DEF")), result.paragraphs)
        assertEquals(1, result.cursorParagraphOffset)
        assertEquals(0, result.cursorTextOffset)
    }

    @Test
    fun `pasting multiline text lands cursor at paste boundary`() {
        val paragraph = NoteParagraph("ABCDEF")

        // Cursor between C and D, paste "123\n456" -> "ABC123" / "456DEF".
        // Cursor should land in second paragraph after "456" (offset 3).
        val result = applyParagraphTextChange(paragraph, "ABC123\n456DEF", cursorOffset = 10)

        assertEquals(listOf(paragraph.copy(text = "ABC123"), NoteParagraph("456DEF")), result.paragraphs)
        assertEquals(1, result.cursorParagraphOffset)
        assertEquals(3, result.cursorTextOffset)
    }

    @Test
    fun `consecutive newlines land cursor at newest empty paragraph`() {
        val paragraph = NoteParagraph("A")

        // "A" then two enters -> "A" / "" / "" with cursor at newest empty line start.
        val result = applyParagraphTextChange(paragraph, "A\n\n", cursorOffset = 3)

        assertEquals(
            listOf(paragraph.copy(text = "A"), NoteParagraph(""), NoteParagraph("")),
            result.paragraphs
        )
        assertEquals(2, result.cursorParagraphOffset)
        assertEquals(0, result.cursorTextOffset)
    }

    @Test
    fun `backspace at paragraph start keeps previous attributes and focuses join`() {
        val first = NoteParagraph("Bold", bold = true, textColor = ParagraphTextColor.BLUE)
        val second = NoteParagraph(" plain", italic = true, alignment = ParagraphAlignment.CENTER)

        val change = applyParagraphBoundaryDeletion(listOf(first, second), index = 1, deleteBackward = true)

        assertEquals(listOf(first.copy(text = "Bold plain")), change?.paragraphs)
        assertEquals(first.id, change?.paragraphs?.first()?.id)
        assertEquals(0, change?.focusedParagraph)
        assertEquals(4, change?.cursorOffset)
    }

    @Test
    fun `delete at paragraph end keeps leading attributes and focuses join`() {
        val first = NoteParagraph("Item", listStyle = ParagraphListStyle.NUMBERED, indent = 1)
        val second = NoteParagraph(" two", todo = true, completed = true)

        val change = applyParagraphBoundaryDeletion(listOf(first, second), index = 0, deleteBackward = false)

        assertEquals(listOf(first.copy(text = "Item two")), change?.paragraphs)
        assertEquals(first.id, change?.paragraphs?.first()?.id)
        assertEquals(0, change?.focusedParagraph)
        assertEquals(4, change?.cursorOffset)
    }

    @Test
    fun `splitting an empty todo does not spawn another todo row`() {
        val paragraph = NoteParagraph("", todo = true)

        // Enter on a blank checkbox: the new row must be a plain paragraph, otherwise
        // holding Enter breeds an endless stack of empty checkboxes.
        val result = applyParagraphTextChange(paragraph, "\n", cursorOffset = 1)

        assertEquals(2, result.paragraphs.size)
        assertTrue(result.paragraphs[0].todo)
        assertFalse(result.paragraphs[1].todo)
        assertEquals(1, result.cursorParagraphOffset)
        assertEquals(0, result.cursorTextOffset)
    }

    @Test
    fun `splitting an empty list row does not spawn another bullet`() {
        listOf(ParagraphListStyle.BULLET, ParagraphListStyle.NUMBERED).forEach { listStyle ->
            val paragraph = NoteParagraph("", listStyle = listStyle)

            val result = applyParagraphTextChange(paragraph, "\n", cursorOffset = 1)

            assertEquals(listStyle, result.paragraphs[0].listStyle)
            assertEquals(ParagraphListStyle.NONE, result.paragraphs[1].listStyle)
        }
    }

    @Test
    fun `splitting a todo that still has content keeps the new row a todo`() {
        val paragraph = NoteParagraph("Buy milk", todo = true)

        val result = applyParagraphTextChange(paragraph, "Buy milk\n", cursorOffset = 9)

        assertTrue(result.paragraphs[0].todo)
        assertTrue(result.paragraphs[1].todo)
        assertFalse(result.paragraphs[1].completed)
    }

    @Test
    fun `format update targets the exact paragraph by id even after index shift`() {
        val p0 = NoteParagraph("First")
        val p1 = NoteParagraph("Second", bold = true)
        val p2 = NoteParagraph("Third", italic = true)

        // Simulate a format button capturing p2.id at composition time, then
        // applying a transform. The transform must target p2 regardless of any
        // index shift in the list (e.g. if a paragraph was inserted or removed).
        val capturedId = p2.id
        val index = listOf(p0, p1, p2).indexOfFirst { it.id == capturedId }
        assertEquals(2, index)

        val updated = listOf(p0, p1, p2).toMutableList().also { list ->
            list[index] = list[index].copy(italic = false)
        }
        assertFalse(updated[2].italic)
        assertTrue(updated[1].bold)
        assertFalse(updated[0].italic)
    }

    @Test
    fun `split allocates a fresh id so focus can target the new paragraph`() {
        val paragraph = NoteParagraph("ABCDEF")

        val result = applyParagraphTextChange(paragraph, "ABC\nDEF", cursorOffset = 4)

        assertEquals(paragraph.id, result.paragraphs[0].id)
        assertTrue(result.paragraphs[1].id != paragraph.id)
        // The focus target resolved by id must be the paragraph holding the caret.
        assertEquals(result.paragraphs[1].id, result.paragraphs[result.cursorParagraphOffset].id)
    }

    @Test
    fun `paragraphTextStyleFrom with italic true produces FontStyle Italic and geometric skew`() {
        val base = TextStyle(fontSize = 16.sp, lineHeight = 20.sp)
        val paragraph = NoteParagraph("test", italic = true, bold = false)
        val style = paragraphTextStyleFrom(
            baseStyle = base,
            paragraph = paragraph,
            color = Color.Black,
            fontSizeScale = 1f,
            decorations = emptyList()
        )
        assertEquals(FontStyle.Italic, style.fontStyle)
        assertEquals(FontWeight.Normal, style.fontWeight)
        assertEquals(TextGeometricTransform(skewX = -0.2f), style.textGeometricTransform)
    }

    @Test
    fun `paragraphTextStyleFrom with italic false produces FontStyle Normal and no skew`() {
        val base = TextStyle(fontSize = 16.sp, lineHeight = 20.sp)
        val paragraph = NoteParagraph("test", italic = false, bold = false)
        val style = paragraphTextStyleFrom(
            baseStyle = base,
            paragraph = paragraph,
            color = Color.Black,
            fontSizeScale = 1f,
            decorations = emptyList()
        )
        assertEquals(FontStyle.Normal, style.fontStyle)
        assertEquals(FontWeight.Normal, style.fontWeight)
        assertEquals(null, style.textGeometricTransform)
    }

    @Test
    fun `paragraphTextStyleFrom with bold true retains FontWeight Bold and italic skew`() {
        val base = TextStyle(fontSize = 16.sp, lineHeight = 20.sp)
        val paragraph = NoteParagraph("test", bold = true, italic = true)
        val style = paragraphTextStyleFrom(
            baseStyle = base,
            paragraph = paragraph,
            color = Color.Black,
            fontSizeScale = 1f,
            decorations = emptyList()
        )
        assertEquals(FontStyle.Italic, style.fontStyle)
        assertEquals(FontWeight.Bold, style.fontWeight)
        assertEquals(TextGeometricTransform(skewX = -0.2f), style.textGeometricTransform)
    }

    @Test
    fun `paragraphTextStyleFrom with decorations produces combined textDecoration`() {
        val base = TextStyle(fontSize = 16.sp, lineHeight = 20.sp)
        val paragraph = NoteParagraph("test", italic = true, strikethrough = true, underline = true)
        val style = paragraphTextStyleFrom(
            baseStyle = base,
            paragraph = paragraph,
            color = Color.Black,
            fontSizeScale = 1f,
            decorations = listOf(TextDecoration.LineThrough, TextDecoration.Underline)
        )
        assertTrue(style.textDecoration != TextDecoration.None)
    }

    @Test
    fun `split paragraphs survive save and load round trip with metadata`() {
        val originalBody = setNoteBodyNotebook("Old body", "Work")
        val paragraph = NoteParagraph(
            text = "FirstSecond",
            style = ParagraphStyle.NOTE,
            todo = true,
            listStyle = ParagraphListStyle.BULLET,
            alignment = ParagraphAlignment.CENTER,
            indent = 2,
            highlighted = true,
            textColor = ParagraphTextColor.GREEN,
            fontSize = ParagraphFontSize.LARGE
        )
        val changed = applyParagraphTextChange(paragraph, "First\nSecond", cursorOffset = 6).paragraphs

        val savedBody = replaceNoteBodyContent(originalBody, serializeNoteBody(changed))

        assertEquals("Work", notebookFromNoteBody(savedBody))
        assertEquals(changed, parseNoteBody(savedBody))
    }
}
