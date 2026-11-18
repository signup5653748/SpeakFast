package com.debk007.speakfast.presentation.ui

import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val prevLocaleInteractionSource = remember { MutableInteractionSource() }
    val nextLocaleInteractionSource = remember { MutableInteractionSource() }
    val prevVoiceInteractionSource = remember { MutableInteractionSource() }
    val nextVoiceInteractionSource = remember { MutableInteractionSource() }
    val dismissErrorInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        val errorMessage = uiState.errorMessage
        if (errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                IconButton(
                    onClick = { viewModel.clearError() },
                    interactionSource = dismissErrorInteractionSource,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Dismiss error message" }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss error",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (locales.isEmpty()) {
            Text("Loading TTS languages...", style = MaterialTheme.typography.bodyLarge)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Language: ${currentLocale.displayLanguage}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val idx = (currentLocaleIndex - 1 + locales.size) % locales.size
                            viewModel.updateLanguage(locales[idx].toLanguageTag())
                        },
                        interactionSource = prevLocaleInteractionSource,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Previous language" }
                    ) {
                        Text("◀", fontSize = 20.sp, modifier = Modifier.semantics { contentDescription = "Previous language" })
                    }
                    Button(
                        onClick = {
                            val idx = (currentLocaleIndex + 1) % locales.size
                            viewModel.updateLanguage(locales[idx].toLanguageTag())
                        },
                        interactionSource = nextLocaleInteractionSource,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Next language" }
                    ) {
                        Text("▶", fontSize = 20.sp, modifier = Modifier.semantics { contentDescription = "Next language" })
                    }
                }
            }

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
                    style = MaterialTheme.typography.bodyLarge
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
                        enabled = voices.isNotEmpty(),
                        interactionSource = prevVoiceInteractionSource,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Previous voice variant" }
                    ) {
                        Text("◀", fontSize = 20.sp, modifier = Modifier.semantics { contentDescription = "Previous voice variant" })
                    }
                    Button(
                        onClick = {
                            if (voices.isNotEmpty()) {
                                val idx = (currentVoiceIndex + 1) % voices.size
                                viewModel.updateVoice(voices[idx].name)
                            }
                        },
                        enabled = voices.isNotEmpty(),
                        interactionSource = nextVoiceInteractionSource,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Next voice variant" }
                    ) {
                        Text("▶", fontSize = 20.sp, modifier = Modifier.semantics { contentDescription = "Next voice variant" })
                    }
                }
            }
        }

        // FLOATING BUTTON SIZE
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Floating Button Size: ${String.format("%.2f", settings?.floatingButtonSize ?: 1.0f)}x",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val current = settings?.floatingButtonSize ?: 1.0f
                            viewModel.updateFloatingButtonSize((current - 0.1f).coerceAtLeast(0.5f))
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .semantics { contentDescription = "Decrease floating button size" }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Decrease",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            val current = settings?.floatingButtonSize ?: 1.0f
                            viewModel.updateFloatingButtonSize((current + 0.1f).coerceAtMost(2.0f))
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .semantics { contentDescription = "Increase floating button size" }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Slider(
                value = settings?.floatingButtonSize ?: 1.0f,
                onValueChange = { viewModel.updateFloatingButtonSize(it) },
                valueRange = 0.5f..2.0f,
                steps = 14,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Adjust floating button size" }
            )
        }

        // SPEECH RATE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Speech Rate: ${String.format("%.1f", settings?.speechRate ?: 1.0f)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = settings?.speechRate ?: 1.0f,
                onValueChange = { viewModel.updateSpeechRate(it) },
                valueRange = 0.5f..2.0f,
                steps = 15,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
                    .semantics { contentDescription = "Adjust speech rate" }
            )
        }

        // PITCH
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pitch: ${String.format("%.1f", settings?.pitch ?: 1.0f)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = settings?.pitch ?: 1.0f,
                onValueChange = { viewModel.updatePitch(it) },
                valueRange = 0.5f..2.0f,
                steps = 15,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
                    .semantics { contentDescription = "Adjust pitch" }
            )
        }

        // READ EXACT SELECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Read Exact Selection",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = settings?.readExactSelection ?: true,
                onCheckedChange = { viewModel.updateReadExactSelection(it) },
                modifier = Modifier
                    .semantics { contentDescription = "Toggle read exact selection" }
                    .padding(16.dp)
            )
        }

        // OCR MODE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OCR Mode (Read Images)",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = settings?.ocrEnabled ?: false,
                onCheckedChange = { viewModel.updateOcrEnabled(it) },
                modifier = Modifier
                    .semantics { contentDescription = "Toggle OCR mode" }
                    .padding(16.dp)
            )
        }
    }
}
