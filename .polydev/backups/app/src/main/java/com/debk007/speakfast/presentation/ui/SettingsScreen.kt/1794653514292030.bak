package com.debk007.speakfast.presentation.ui

import android.speech.tts.Voice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgeDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.debk007.speakfast.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val locales = uiState.availableLocales
    val voices = viewModel.voicesForSelectedLocale()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        // Language selector
        val currentLocale = locales.find { it.toLanguageTag() == (settings?.languageCode ?: "en-US") }
            ?: locales.firstOrNull()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Language: ${currentLocale?.displayLanguage ?: "Unknown"}", style = MaterialTheme.typography.bodyLarge)
            Row {
                Button(onClick = {
                    val idx = (locales.indexOf(currentLocale) - 1 + locales.size) % locales.size
                    viewModel.updateLanguage(locales[idx].toLanguageTag())
                }) { Text("◀") }
                Button(onClick = {
                    val idx = (locales.indexOf(currentLocale) + 1) % locales.size
                    viewModel.updateLanguage(locales[idx].toLanguageTag())
                }) { Text("▶") }
            }
        }

        // Voice selector with online/offline badges
        val currentVoice = voices.find { it.name == (settings?.voice ?: "") }
            ?: voices.firstOrNull()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Badge(
                badge = {
                    if (viewModel.isOnlineVoice(currentVoice)) {
                        Text("Online", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    } else {
                        Text("Offline", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                },
                badgeColors = if (viewModel.isOnlineVoice(currentVoice))
                    BadgeDefaults.badgeColors(containerColor = Color(0xFF1E88E5))
                else
                    BadgeDefaults.badgeColors(containerColor = Color(0xFF43A047))
            ) {
                Text("Voice: ${currentVoice?.name ?: "Default"}", style = MaterialTheme.typography.bodyLarge)
            }
            Row {
                Button(onClick = {
                    val idx = (voices.indexOf(currentVoice) - 1 + voices.size) % voices.size
                    viewModel.updateVoice(voices[idx].name)
                }) { Text("◀") }
                Button(onClick = {
                    val idx = (voices.indexOf(currentVoice) + 1) % voices.size
                    viewModel.updateVoice(voices[idx].name)
                }) { Text("▶") }
            }
        }

        Text("Speech Rate: ${String.format("%.1f", settings?.speechRate ?: 1.0f)}", style = MaterialTheme.typography.bodyLarge)
        Text("Pitch: ${String.format("%.1f", settings?.pitch ?: 1.0f)}", style = MaterialTheme.typography.bodyLarge)
        Text("Trigger: ${settings?.triggerMethod?.name ?: "QUICK_SETTINGS_TILE"}", style = MaterialTheme.typography.bodyLarge)
    }
}