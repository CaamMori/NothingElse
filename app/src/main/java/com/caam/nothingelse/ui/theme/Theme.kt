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
private val Night = Color(0xFF101113)
private val Blue = Color(0xFF0A70C7)

private val LightPalette = lightColorScheme(
    primary = Blue, onPrimary = Color.White, background = Paper, surface = Paper,
    surfaceVariant = Color(0xFFF2EFE8), onBackground = Ink, onSurface = Ink,
    onSurfaceVariant = Color(0xFF625F5A), outline = Color(0xFFB8B3AA),
    outlineVariant = Color(0xFFE5E1DA), error = Color(0xFFB3261E), onError = Color.White
)
private val DarkPalette = darkColorScheme(
    primary = Color(0xFF9DCEFF), onPrimary = Color(0xFF003258), background = Night,
    surface = Color(0xFF191B1E), surfaceVariant = Color(0xFF24272B),
    onBackground = Color.White, onSurface = Color.White,
    onSurfaceVariant = Color(0xFFC4C7C9), outline = Color(0xFF909498),
    outlineVariant = Color(0xFF41454A), error = Color(0xFFFFB4AB), onError = Color(0xFF690005)
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
