package com.beast.app

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun parseHex(hex: String): Color = try {
    Color(AndroidColor.parseColor(hex))
} catch (_: Throwable) { Color(0xFF2E7D32) }

@Composable
fun BeastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentHex: String = "#2E7D32",
    content: @Composable () -> Unit
) {
    val primary = parseHex(accentHex)
    val light = lightColorScheme(
        primary = primary,
        secondary = primary.copy(alpha = 0.90f),
        tertiary = primary.copy(alpha = 0.80f),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color(0xFFFFFFFF),
        onBackground = Color(0xFF121212),
        onSurface = Color(0xFF121212),
    )
    val dark = darkColorScheme(
        primary = primary,
        secondary = primary.copy(alpha = 0.85f),
        tertiary = primary.copy(alpha = 0.75f),
    )
    MaterialTheme(colorScheme = if (darkTheme) dark else light, content = content)
}
