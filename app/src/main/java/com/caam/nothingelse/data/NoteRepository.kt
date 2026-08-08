package com.caam.nothingelse.data

class NoteRepository(private val dao: NoteDao) {
    fun getAll() = dao.getAll()
    fun get(id: Long) = dao.get(id)
    fun insert(note: Note) = dao.insert(note)
    fun update(note: Note) = dao.update(note)
    fun delete(note: Note) = dao.delete(note)
}
