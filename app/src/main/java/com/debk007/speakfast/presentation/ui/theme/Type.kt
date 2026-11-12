package com.debk007.speakfast.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

private val provider = FontFamily(
    Font("Roboto", FontWeight.Normal)
)

val SpeakFastTypography = Typography(
    displayLarge = Typography().displayLarge.copy(
        fontFamily = provider,
        fontSize = 57.sp
    ),
    displayMedium = Typography().displayMedium.copy(
        fontFamily = provider,
        fontSize = 52.sp
    ),
    displaySmall = Typography().displaySmall.copy(
        fontFamily = provider,
        fontSize = 46.sp
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = provider,
        fontSize = 32.sp
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = provider,
        fontSize = 28.sp
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = provider,
        fontSize = 24.sp
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = provider,
        fontSize = 22.sp
    ),
    titleMedium = Typography().titleMedium.copy(
        fontFamily = provider,
        fontSize = 16.sp
    ),
    titleSmall = Typography().titleSmall.copy(
        fontFamily = provider,
        fontSize = 14.sp
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = provider,
        fontSize = 16.sp
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = provider,
        fontSize = 14.sp
    ),
    bodySmall = Typography().bodySmall.copy(
        fontFamily = provider,
        fontSize = 12.sp
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = provider,
        fontSize = 14.sp
    ),
    labelMedium = Typography().labelMedium.copy(
        fontFamily = provider,
        fontSize = 12.sp
    ),
    labelSmall = Typography().labelSmall.copy(
        fontFamily = provider,
        fontSize = 11.sp
    )
)