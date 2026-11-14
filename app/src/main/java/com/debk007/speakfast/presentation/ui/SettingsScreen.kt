package com.debk007.speakfast.presentation.ui

import android.speech.tts.Voice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debk007.speakfast.presentation.viewmodel.SettingsViewModel
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val locales by viewModel.ttsLocales.collectAsState()
    val settings = uiState.settings

    val currentLocale = locales.firstOrNull { it.toLanguageTag() == (settings?.languageCode ?: "") }
        ?: Locale.forLanguageTag(settings?.languageCode ?: "en-US")
    val voices: List<Voice> = viewModel.voicesFor(currentLocale)
    val currentVoice = voices.firstOrNull { it.name == (settings?.voice ?: "") } ?: voices.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .semantics { hideFromAccessibility() },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        if (locales.isEmpty()) {
            Text("Loading TTS languages...", style = MaterialTheme.typography.bodyLarge)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Language: ${currentLocale.displayLanguage}", style = MaterialTheme.typography.bodyLarge)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = currentVoice?.let {
                    "${it.name} [${if (viewModel.isOnline(it)) "online" else "offline"}]"
                } ?: "No voices"
                Text("Voice: $label", style = MaterialTheme.typography.bodySmall)
                Row {
                    Button(onClick = {
                        if (voices.isNotEmpty()) {
                            val idx = (voices.indexOf(currentVoice) - 1 + voices.size) % voices.size
                            viewModel.updateVoice(voices[idx].name)
                        }
                    }) { Text("◀") }
                    Button(onClick = {
                        if (voices.isNotEmpty()) {
                            val idx = (voices.indexOf(currentVoice) + 1) % voices.size
                            viewModel.updateVoice(voices[idx].name)
                        }
                    }) { Text("▶") }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Read exact selection", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = settings?.readExactSelection ?: true,
                onCheckedChange = { viewModel.updateReadExactSelection(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OCR Mode (read images)", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = settings?.ocrEnabled ?: false,
                onCheckedChange = { viewModel.updateOcrEnabled(it) }
            )
        }

        Text("Speech Rate: ${String.format("%.1f", settings?.speechRate ?: 1.0f)}", style = MaterialTheme.typography.bodyLarge)
        Text("Pitch: ${String.format("%.1f", settings?.pitch ?: 1.0f)}", style = MaterialTheme.typography.bodyLarge)
    }
}