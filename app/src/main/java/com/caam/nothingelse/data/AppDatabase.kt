package com.caam.nothingelse.data

import androidx.room.Database
import androidx.room.RoomDatabase

// Version and entity are deliberately unchanged: this opens the existing v1 database in place.
@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
