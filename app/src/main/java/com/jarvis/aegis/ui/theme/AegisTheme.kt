package com.jarvis.aegis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AegisBlack = Color(0xFF000000)
val AegisWhite = Color(0xFFFFFFFF)
val AegisPurple = Color(0xFF9B5CFF)
val AegisCrimson = Color(0xFFD32F2F)

private val Colors = darkColorScheme(
    primary = AegisWhite,
    onPrimary = AegisBlack,
    secondary = AegisPurple,
    error = AegisCrimson,
    background = AegisBlack,
    onBackground = AegisWhite,
    surface = AegisBlack,
    onSurface = AegisWhite,
)

@Composable
fun AegisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Colors,
        typography = MaterialTheme.typography.copy(
            bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            titleLarge = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
        ),
        content = content,
    )
}
