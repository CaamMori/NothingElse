package com.caam.nothingelse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF0D0D0F)
private val Paper = Color(0xFFFFFEFA)
private val Night = Color(0xFF111112)
private val Blue = Color(0xFF0A70C7)

private val LightPalette = lightColorScheme(
    primary = Blue, background = Paper, surface = Paper, onBackground = Ink, onSurface = Ink,
    onSurfaceVariant = Color(0xFF74716C), outlineVariant = Color(0xFFE5E1DA), error = Color(0xFFB3261E)
)
private val DarkPalette = darkColorScheme(
    primary = Color(0xFF8FC7FF), background = Night, surface = Night, onBackground = Color(0xFFF4F1EA),
    onSurface = Color(0xFFF4F1EA), onSurfaceVariant = Color(0xFFA9A6A1), outlineVariant = Color(0xFF343332), error = Color(0xFFFFB4AB)
)

private val QuietType = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 29.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
)

@Composable
fun NothingElseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkPalette else LightPalette,
        typography = QuietType,
        content = content
    )
}
