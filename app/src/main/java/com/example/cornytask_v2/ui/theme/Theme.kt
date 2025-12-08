package com.example.cornytask_v2.ui.theme

import android.app.Activity
import android.os.Build
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
    primary = DeepPink,
    secondary = SoftPink,
    tertiary = LightPink
)

private val LightColorScheme = lightColorScheme(
    primary = SoftPink, // Main button color
    onPrimary = Purple40, // Text on main buttons
    primaryContainer = SoftPink, // FAB color
    onPrimaryContainer = Purple40, // Icon on FAB
    secondary = DeepPink,
    tertiary = LightPink,
    background = Color.White,
    surface = Color.White,
    surfaceVariant = LightPink, // For cards, inactive elements
    onSurfaceVariant = Purple40 // Text on cards, etc.
)

@Composable
fun Cornytaskv2Theme(
    darkTheme: Boolean = false, // Always use light theme
    dynamicColor: Boolean = false, // Disabled to use our custom theme
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Always use our custom light scheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}