package com.example.laburin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = LightBackground,
    surface = LightBackground,
)

/**
 * Always the app's own light colors. The screens use fixed light colors (white panels, grey text), so following the
 * phone's dark mode or its wallpaper colors (dynamic color, Android 12+) mixed white and black screens and changed
 * the buttons from phone to phone. A real dark mode needs those fixed colors moved into the theme first.
 */
@Composable
fun LaburinTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColorScheme, typography = Typography, content = content)
}
