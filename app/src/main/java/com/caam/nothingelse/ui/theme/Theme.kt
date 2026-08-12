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

private val Ink = Color(0xFF111111)
private val Paper = Color(0xFFFAF9F6)
private val Night = Color(0xFF151618)
private val Blue = Color(0xFF1769AA)

private val LightPalette = lightColorScheme(
    primary = Blue, onPrimary = Color.White, background = Paper, surface = Paper,
    surfaceVariant = Color(0xFFF1F0EC), onBackground = Ink, onSurface = Ink,
    onSurfaceVariant = Color(0xFF666666), outline = Color(0xFFB8B7B3),
    outlineVariant = Color(0xFFEAEAEA), error = Color(0xFFB3261E), onError = Color.White
)
private val DarkPalette = darkColorScheme(
    primary = Color(0xFF8EC7FF), onPrimary = Color(0xFF002F50), background = Night,
    surface = Color(0xFF1B1C1F), surfaceVariant = Color(0xFF242529),
    onBackground = Color(0xFFF3F1EC), onSurface = Color(0xFFF3F1EC),
    onSurfaceVariant = Color(0xFFB5B4B0), outline = Color(0xFF77787C),
    outlineVariant = Color(0xFF323338), error = Color(0xFFFFB4AB), onError = Color(0xFF690005)
)

private val QuietType = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
)

@Composable
fun NothingElseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkPalette else LightPalette,
        typography = QuietType,
        content = content
    )
}
