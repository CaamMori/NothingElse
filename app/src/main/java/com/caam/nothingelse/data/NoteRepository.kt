package com.caam.nothingelse.data

class NoteRepository(private val notes: NoteDao) {
    fun activeNotes() = notes.activeNotes()
    fun archivedNotes() = notes.archivedNotes()
    fun create(note: Note) = notes.insert(note)
    fun save(note: Note) = notes.update(note)
    fun remove(note: Note) = notes.delete(note)
}
