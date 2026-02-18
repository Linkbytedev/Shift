package com.Linkbyte.Shift.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = White,
    primaryContainer = AccentBlue.copy(alpha = 0.15f),
    onPrimaryContainer = AccentBlue,

    secondary = AccentTeal,
    onSecondary = DarkBg,
    secondaryContainer = AccentTeal.copy(alpha = 0.15f),
    onSecondaryContainer = AccentTeal,

    tertiary = AccentPurple,

    background = DarkBg,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkElevated,
    onSurfaceVariant = TextSecondary,

    outline = DarkBorder,
    error = Error
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccentBlue,
    onPrimary = White,
    primaryContainer = LightAccentBlue.copy(alpha = 0.1f),
    onPrimaryContainer = LightAccentBlue,

    secondary = LightAccentTeal,
    onSecondary = White,
    secondaryContainer = LightAccentTeal.copy(alpha = 0.1f),
    onSecondaryContainer = LightAccentTeal,

    tertiary = LightAccentPurple,

    background = LightBg,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevated,
    onSurfaceVariant = LightTextSecondary,

    outline = LightBorder,
    error = ErrorRed
)

@Composable
fun ShiftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
