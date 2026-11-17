package com.debk007.speakfast.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.getColor

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF003171),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283041),
    background = Color(0xFF1B1B1F),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1B1B1F),
    onSurface = Color(0xFFE3E2E6)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF5C6BC0),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1D1D1D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1D1D)
)

@Composable
fun SpeakFastTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isDarkTheme = (context.resources.configuration.uiMode and 
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    
    val colorScheme = if (isDarkTheme) DarkColors else LightColors
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}