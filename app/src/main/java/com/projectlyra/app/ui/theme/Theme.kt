package com.projectlyra.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LyraDarkColorScheme = darkColorScheme(
    primary = Ember,
    onPrimary = Mist,
    secondary = Skyline,
    tertiary = Emerald,
    background = Midnight,
    onBackground = Mist,
    surface = Charcoal,
    onSurface = Mist,
    surfaceVariant = Slate,
    onSurfaceVariant = Steel,
)

@Composable
fun ProjectLyraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LyraDarkColorScheme,
        typography = Typography,
        content = content,
    )
}
