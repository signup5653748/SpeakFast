package com.debk007.speakfast.presentation.ui

import android.speech.tts.Voice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

    val currentLocale = locales.firstOrNull { it.toLanguageTag() == (settings?.languageCode ?: "") }
        ?: Locale.forLanguageTag(settings?.languageCode ?: "en-US")
    val voices: List<Voice> = viewModel.voicesFor(currentLocale)
    val currentVoice = voices.firstOrNull { it.name == (settings?.voice ?: "") } ?: voices.firstOrNull()

    val currentLocaleIndex = locales.indexOf(currentLocale).coerceAtLeast(0)
    val currentVoiceIndex = voices.indexOf(currentVoice).coerceAtLeast(0)

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
                Row {
                    Button(
                        onClick = {
                            val nextIndex = (currentLocaleIndex - 1 + locales.size) % locales.size
                            viewModel.updateLanguage(locales[nextIndex].toLanguageTag())
                        }
                    ) {
                        Text("◀", modifier = Modifier.semantics { contentDescription = "Previous language" })
                    }
                    Button(
                        onClick = {
                            val nextIndex = (currentLocaleIndex + 1) % locales.size
                            viewModel.updateLanguage(locales[nextIndex].toLanguageTag())
                        }
                    ) {
                        Text("▶", modifier = Modifier.semantics { contentDescription = "Next language" })
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = currentVoice?.let { "${it.name} [${if (viewModel.isOnline(it)) "online" else "offline"}]" } ?: "No voices"
                Text(
                    text = "Voice: $label",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    Button(
                        onClick = {
                            if (voices.isNotEmpty()) {
                                val nextIndex = (currentVoiceIndex - 1 + voices.size) % voices.size
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
                                val nextIndex = (currentVoiceIndex + 1) % voices.size
                                viewModel.updateVoice(voices[nextIndex].name)
                            }
                        },
                        enabled = voices.isNotEmpty()
                    ) {
                        Text("▶", modifier = Modifier.semantics { contentDescription = "Next voice" })
                    }
                }
            }

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