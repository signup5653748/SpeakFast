package com.debk007.speakfast.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF003171),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283041),
    background = Color(0xFF1B1B1F),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE3E2E6),
    error = Color(0xFFCF6679),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF5C6BC0),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1D1D1D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1D1D),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun SpeakFastTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isDarkTheme = (context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES

    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}