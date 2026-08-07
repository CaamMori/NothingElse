package com.caam.nothingelse.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.caam.nothingelse.data.Note

@Composable
fun EditNoteScreen(note: Note, onSave: (Note) -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var body by remember { mutableStateOf(note.body) }
    Column(modifier = Modifier.fillMaxSize()) {
        BasicTextField(value = title, onValueChange = { title = it })
        BasicTextField(value = body, onValueChange = { body = it })
        Button(onClick = { onSave(note.copy(title = title, body = body)) }) { Text("Save") }
    }
}
