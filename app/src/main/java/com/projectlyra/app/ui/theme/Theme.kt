package com.projectlyra.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
fun ProjectLyraTheme(
    dynamicAccentEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = if (
        dynamicAccentEnabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        dynamicDarkColorScheme(context)
    } else {
        LyraDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
