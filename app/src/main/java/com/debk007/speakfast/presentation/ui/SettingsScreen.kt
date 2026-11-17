package com.debk007.speakfast.presentation.ui

import android.speech.tts.Voice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

    // FIX (language fallback edge case): if the saved languageCode is not in the
    // TTS locale list, indexOf returns -1. coerceAtLeast(0) maps -1 -> 0 so the
    // ◀/▶ buttons always wrap around the available locales and never index
    // negatively or jump to arbitrary locales.
    val currentLocale = locales.firstOrNull { it.toLanguageTag() == (settings?.languageCode ?: "") }
        ?: Locale.forLanguageTag(settings?.languageCode ?: "en-US")
    val voices: List<Voice> = viewModel.voicesFor(currentLocale)
    val currentVoice = voices.firstOrNull { it.name == (settings?.voice ?: "") } ?: voices.firstOrNull()
    val currentLocaleIndex = locales.indexOf(currentLocale).coerceAtLeast(0)
    val currentVoiceIndex = voices.indexOf(currentVoice).coerceAtLeast(0)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // FIX (error messages): show the ViewModel's errorMessage as a dismissible
            // red banner at the top of the screen.
            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (locales.isEmpty()) {
                Text(
                    text = "Loading TTS languages...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                // LANGUAGE SETTING
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Language: ${currentLocale.displayLanguage}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val idx = (currentLocaleIndex - 1 + locales.size) % locales.size
                                viewModel.updateLanguage(locales[idx].toLanguageTag())
                            }
                        ) {
                            Text(
                                "◀",
                                modifier = Modifier.semantics { contentDescription = "Previous language" }
                            )
                        }
                        Button(
                            onClick = {
                                val idx = (currentLocaleIndex + 1) % locales.size
                                viewModel.updateLanguage(locales[idx].toLanguageTag())
                            }
                        ) {
                            Text(
                                "▶",
                                modifier = Modifier.semantics { contentDescription = "Next language" }
                            )
                        }
                    }
                }

                // VOICE VARIANT SETTING
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val voiceLabel = currentVoice?.let {
                        "${it.name} [${if (viewModel.isOnline(it)) "online" else "offline"}]"
                    } ?: "No voices available"
                    Text(
                        text = "Voice Variant: $voiceLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (voices.isNotEmpty()) {
                                    val idx = (currentVoiceIndex - 1 + voices.size) % voices.size
                                    viewModel.updateVoice(voices[idx].name)
                                }
                            },
                            enabled = voices.isNotEmpty()
                        ) {
                            Text(
                                "◀",
                                modifier = Modifier.semantics { contentDescription = "Previous voice" }
                            )
                        }
                        Button(
                            onClick = {
                                if (voices.isNotEmpty()) {
                                    val idx = (currentVoiceIndex + 1) % voices.size
                                    viewModel.updateVoice(voices[idx].name)
                                }
                            },
                            enabled = voices.isNotEmpty()
                        ) {
                            Text(
                                "▶",
                                modifier = Modifier.semantics { contentDescription = "Next voice" }
                            )
                        }
                    }
                }
            }

            // SPEECH RATE SETTING
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        .width(200.dp)
                        .semantics { contentDescription = "Adjust speech rate" }
                )
            }

            // PITCH SETTING
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        .width(200.dp)
                        .semantics { contentDescription = "Adjust pitch" }
                )
            }

            // READ EXACT SELECTION SETTING
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
                    onCheckedChange = { viewModel.updateReadExactSelection(it) },
                    modifier = Modifier.semantics { contentDescription = "Toggle read exact selection" }
                )
            }

            // OCR MODE SETTING
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
                    onCheckedChange = { viewModel.updateOcrEnabled(it) },
                    modifier = Modifier.semantics { contentDescription = "Toggle OCR mode" }
                )
            }
        }
    }
}