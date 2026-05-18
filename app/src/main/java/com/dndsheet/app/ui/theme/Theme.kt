package com.dndsheet.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = InkPrimary,
    secondary = InkSecondary,
    background = ParchmentLight,
    surface = ParchmentLight,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = InkPrimaryDark,
    secondary = InkSecondaryDark,
    background = ParchmentDark,
    surface = ParchmentDark,
    error = ErrorRedDark
)

/**
 * App theme. Uses M3 dynamic color on Android 12+ when [dynamicColor] is
 * true (default); otherwise falls back to the parchment palette in [Color.kt].
 *
 * Annotations and the freehand layer (commit 5) read from MaterialTheme.colorScheme
 * so they pick up the right ink color for the active theme automatically.
 */
@Composable
fun DnDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}
