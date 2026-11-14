package com.debk007.speakfast.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debk007.speakfast.model.Language
import com.debk007.speakfast.model.Voice
import com.debk007.speakfast.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        val currentLang = Language.fromCode(settings?.languageCode ?: "en")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Language: ${currentLang.displayName}", style = MaterialTheme.typography.bodyLarge)
            Row {
                Button(onClick = {
                    val idx = (Language.entries.indexOf(currentLang) - 1 + Language.entries.size) % Language.entries.size
                    viewModel.updateLanguage(Language.entries[idx].code)
                }) { Text("◀") }
                Button(onClick = {
                    val idx = (Language.entries.indexOf(currentLang) + 1) % Language.entries.size
                    viewModel.updateLanguage(Language.entries[idx].code)
                }) { Text("▶") }
            }
        }

        val currentVoice = Voice.entries.firstOrNull { it.voiceId == (settings?.voice ?: "default") } ?: Voice.DEFAULT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Voice: ${currentVoice.displayName}", style = MaterialTheme.typography.bodyLarge)
            Row {
                Button(onClick = {
                    val idx = (Voice.entries.indexOf(currentVoice) - 1 + Voice.entries.size) % Voice.entries.size
                    viewModel.updateVoice(Voice.entries[idx].voiceId)
                }) { Text("◀") }
                Button(onClick = {
                    val idx = (Voice.entries.indexOf(currentVoice) + 1) % Voice.entries.size
                    viewModel.updateVoice(Voice.entries[idx].voiceId)
                }) { Text("▶") }
            }
        }

        Text("Speech Rate: ${String.format("%.1f", settings?.speechRate ?: 1.0f)}", style = MaterialTheme.typography.bodyLarge)
        Text("Pitch: ${String.format("%.1f", settings?.pitch ?: 1.0f)}", style = MaterialTheme.typography.bodyLarge)
        Text("Trigger: ${settings?.triggerMethod?.name ?: "QUICK_SETTINGS_TILE"}", style = MaterialTheme.typography.bodyLarge)
    }
}