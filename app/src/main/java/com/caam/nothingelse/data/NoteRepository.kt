package com.caam.nothingelse.data

class NoteRepository(private val dao: NoteDao) {
    suspend fun getAll() = dao.getAll()
    suspend fun getArchived() = dao.getArchived()
    suspend fun get(id: Long) = dao.get(id)
    suspend fun insert(note: Note) = dao.insert(note)
    suspend fun update(note: Note) = dao.update(note)
    suspend fun delete(note: Note) = dao.delete(note)
}
