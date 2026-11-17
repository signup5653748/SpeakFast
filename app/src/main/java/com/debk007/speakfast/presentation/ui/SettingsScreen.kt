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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.debk007.speakfast.presentation.viewmodel.SettingsViewModel
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val locales by viewModel.ttsLocales.collectAsState()
    val settings = uiState.settings

    // Safe Language Selection - prevents divide by zero when locales is empty
    val safeLocaleIndex = locales.indexOfFirst { it.toLanguageTag() == (settings?.languageCode ?: "") }.coerceAtLeast(0)
    val currentLocale = locales.getOrElse(safeLocaleIndex) { Locale.getDefault() }

    // Safe Voice Selection - prevents divide by zero when voices is empty
    val voices = viewModel.voicesFor(currentLocale)
    val safeVoiceIndex = voices.indexOfFirst { it.name == (settings?.voice ?: "") }.coerceAtLeast(0)
    val currentVoice = voices.getOrElse(safeVoiceIndex) { null }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clearAndSetSemantics { },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            val errorMsg = uiState.errorMessage
            if (!errorMsg.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            }

            // Language Selector with safe cycling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Language: ${currentLocale.displayLanguage} (${currentLocale.displayCountry})",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    Button(
                        onClick = {
                            if (locales.isNotEmpty()) {
                                val nextIndex = (safeLocaleIndex - 1 + locales.size) % locales.size
                                viewModel.updateLanguage(locales[nextIndex].toLanguageTag())
                            }
                        },
                        enabled = locales.isNotEmpty()
                    ) {
                        Text("◀", modifier = Modifier.semantics { contentDescription = "Previous language" })
                    }
                    Button(
                        onClick = {
                            if (locales.isNotEmpty()) {
                                val nextIndex = (safeLocaleIndex + 1) % locales.size
                                viewModel.updateLanguage(locales[nextIndex].toLanguageTag())
                            }
                        },
                        enabled = locales.isNotEmpty()
                    ) {
                        Text("▶", modifier = Modifier.semantics { contentDescription = "Next language" })
                    }
                }
            }

            // Voice Selector with safe cycling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val voiceLabel = currentVoice?.let {
                    "${it.name} (${if (viewModel.isOnline(it)) "Online" else "Offline"})"
                } ?: "No voices available"
                
                Text(
                    text = "Voice: $voiceLabel",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    Button(
                        onClick = {
                            if (voices.isNotEmpty()) {
                                val nextIndex = (safeVoiceIndex - 1 + voices.size) % voices.size
                                viewModel.updateVoice(voices[nextIndex].name)
                            }
                        },
                        enabled = voices.isNotEmpty()
                    ) {
                        Text("◀", modifier = Modifier.semantics { contentDescription = "Previous voice" })
                    }
                    Button(
                        onClick = {
                            if (voices.isNotEmpty()) {
                                val nextIndex = (safeVoiceIndex + 1) % voices.size
                                viewModel.updateVoice(voices[nextIndex].name)
                            }
                        },
                        enabled = voices.isNotEmpty()
                    ) {
                        Text("▶", modifier = Modifier.semantics { contentDescription = "Next voice" })
                    }
                }
            }

            // Speech Rate Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Speech Rate: ${String.format("%.1f", settings?.speechRate ?: 1.0f)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = settings?.speechRate ?: 1.0f,
                    onValueChange = { viewModel.updateSpeechRate(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            // Pitch Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pitch: ${String.format("%.1f", settings?.pitch ?: 1.0f)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = settings?.pitch ?: 1.0f,
                    onValueChange = { viewModel.updatePitch(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            // Read Exact Selection Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Read Exact Selection",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = settings?.readExactSelection ?: true,
                    onCheckedChange = { viewModel.updateReadExactSelection(it) }
                )
            }

            // OCR Mode Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OCR Mode (Read Images)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = settings?.ocrEnabled ?: false,
                    onCheckedChange = { viewModel.updateOcrEnabled(it) }
                )
            }
        }
    }
}