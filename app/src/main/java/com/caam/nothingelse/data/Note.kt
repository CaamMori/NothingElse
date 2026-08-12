package com.caam.nothingelse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The v1 persisted shape. Field and table names must remain stable for existing notes. */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val title: String = "",
    val body: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val archived: Boolean = false
)
