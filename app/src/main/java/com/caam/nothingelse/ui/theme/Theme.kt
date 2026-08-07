package com.caam.nothingelse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun NothingElseTheme(content: @Composable() () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        content()
    }
}

