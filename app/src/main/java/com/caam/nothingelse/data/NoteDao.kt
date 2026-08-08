package com.caam.nothingelse.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE archived = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun activeNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE archived = 1 ORDER BY updatedAt DESC")
    fun favoriteNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun note(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(note: Note): Long

    @Update
    fun update(note: Note): Int

    @Delete
    fun delete(note: Note): Int

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Long>): Int
}
