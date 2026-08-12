package com.caam.nothingelse

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteOrderTest {
    @Test
    fun `final visible order is merged into current global order slots`() {
        val currentIds = listOf(1L, 2L, 3L, 4L, 5L)
        val finalVisibleIds = listOf(4L, 2L, 3L)

        assertEquals(listOf(1L, 4L, 2L, 3L, 5L), mergeVisibleNoteOrder(currentIds, finalVisibleIds))
    }

    @Test
    fun `notes outside the visible reorder remain in place`() {
        val currentIds = listOf(10L, 1L, 20L, 2L, 3L, 30L)
        val finalVisibleIds = listOf(3L, 1L, 2L)

        assertEquals(listOf(10L, 3L, 20L, 1L, 2L, 30L), mergeVisibleNoteOrder(currentIds, finalVisibleIds))
    }

    @Test
    fun `invalid visible order is ignored`() {
        val currentIds = listOf(1L, 2L, 3L)

        assertEquals(currentIds, mergeVisibleNoteOrder(currentIds, listOf(2L, 2L)))
        assertEquals(currentIds, mergeVisibleNoteOrder(currentIds, listOf(2L, 4L)))
    }
}
