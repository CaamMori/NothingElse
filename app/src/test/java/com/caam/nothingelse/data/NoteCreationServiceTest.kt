package com.caam.nothingelse.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NoteCreationServiceTest {
    private val service = NoteCreationService()

    @Test
    fun `import uses first line as title and drops it from the body`() {
        val raw = "# Project notes\n\nBody line one\nBody line two"

        val prepared = service.prepareImport(raw)

        assertEquals("Project notes", prepared.title)
        assertEquals("Body line one\nBody line two", prepared.content)
    }

    @Test
    fun `single line import keeps title and leaves body empty`() {
        val prepared = service.prepareImport("Just one line")

        assertEquals("Just one line", prepared.title)
        assertEquals("", prepared.content)
    }

    @Test
    fun `truncated title keeps the whole first line in the body`() {
        val firstLine = "123456789012345678901234567890extra"
        val raw = "$firstLine\nBody"

        val prepared = service.prepareImport(raw)

        // Title is truncated to 30 chars, so the first line was NOT fully consumed:
        // keeping it in the body avoids silently losing "extra".
        assertEquals("123456789012345678901234567890", prepared.title)
        assertEquals(raw, prepared.content)
    }

    @Test
    fun `untitled import keeps its body`() {
        val prepared = service.prepareImport("\nBody")

        assertEquals("Untitled", prepared.title)
        assertEquals("\nBody", prepared.content)
    }

    @Test
    fun `import title falls back to first thirty characters of first line`() {
        val firstLine = "123456789012345678901234567890extra"

        assertEquals("123456789012345678901234567890", service.prepareImport("$firstLine\nBody").title)
    }

    @Test
    fun `blank import is rejected`() {
        assertThrows(BlankImportException::class.java) { service.prepareImport(" \n\t") }
    }
}
