package com.caam.nothingelse.data

import androidx.room.*

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE archived = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun get(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(note: Note): Long

    @Update
    fun update(note: Note): Int

    @Delete
    fun delete(note: Note): Int
}
