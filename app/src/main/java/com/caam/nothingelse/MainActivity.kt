package com.caam.nothingelse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.caam.nothingelse.data.AppDatabase
import com.caam.nothingelse.data.Note
import androidx.room.Room

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "nothingelse-db").allowMainThreadQueries().build()
        val dao = db.noteDao()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val notes = remember { mutableStateOf(dao.getAll()) }
                    Column {
                        TopAppBar(title = { Text("NothingElse") })
                        Button(onClick = {
                            val id = dao.insert(Note(id = 0, title = "New note", body = ""))
                            notes.value = dao.getAll()
                        }) { Text("New") }
                        LazyColumn {
                            items(notes.value) { n ->
                                androidx.compose.material3.ListItem(
                                    headlineContent = { Text(n.title.ifEmpty { "(no title)" }) },
                                    supportingContent = { Text(n.body.take(80)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
