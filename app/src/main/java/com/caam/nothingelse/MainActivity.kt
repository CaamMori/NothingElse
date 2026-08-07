package com.caam.nothingelse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("notes", MODE_PRIVATE)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val ctx = LocalContext.current
                    val note = remember { mutableStateOf(prefs.getString("note", "") ?: "") }
                    Column {
                        BasicTextField(value = note.value, onValueChange = { note.value = it })
                        Button(onClick = {
                            prefs.edit { putString("note", note.value) }
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

