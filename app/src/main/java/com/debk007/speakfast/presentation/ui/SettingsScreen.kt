package com.debk007.speakfast.presentation.ui

import android.speech.tts.TextToSpeech
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debk007.speakfast.presentation.viewmodel.SettingsViewModel
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val locales by viewModel.ttsLocales.collectAsState()
    val engines by viewModel.ttsEngines.collectAsState()
    val settings = uiState.settings

    // ✅ 1. ROOT SURFACE WRAPPER (Fixes theming & background issues)
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

            // ✅ 2. TTS ENGINE SELECTOR (The missing piece!)
            if (engines.isNotEmpty()) {
                val currentEngineName = settings?.ttsEngine ?: ""
                val currentEngine = engines.firstOrNull { it.name == currentEngineName } ?: engines.firstOrNull()
                val engineIndex = engines.indexOf(currentEngine).coerceAtLeast(0)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TTS Engine: ${currentEngine?.label ?: "System Default"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val idx = (engineIndex - 1 + engines.size) % engines.size
                                viewModel.updateTtsEngine(engines[idx].name)
                            },
                            enabled = engines.size > 1
                        ) { Text("◀", modifier = Modifier.semantics { contentDescription = "Previous TTS engine" }) }
                        Button(
                            onClick = {
                                val idx = (engineIndex + 1) % engines.size
                                viewModel.updateTtsEngine(engines[idx].name)
                            },
                            enabled = engines.size > 1
                        ) { Text("▶", modifier = Modifier.semantics { contentDescription = "Next TTS engine" }) }
                    }
                }
            }

            // ✅ 3. LANGUAGE & VOICE SELECTORS (Crash-safe indexing)
            if (locales.isEmpty()) {
                Text(
                    text = "Loading TTS languages...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                val safeLocaleIndex = locales.indexOfFirst { it.toLanguageTag() == (settings?.languageCode ?: "") }.coerceAtLeast(0)
                val currentLocale = locales.getOrElse(safeLocaleIndex) { Locale.getDefault() }
                val voices = viewModel.voicesFor(currentLocale)
                val safeVoiceIndex = voices.indexOfFirst { it.name == (settings?.voice ?: "") }.coerceAtLeast(0)
                val currentVoice = voices.getOrElse(safeVoiceIndex) { null }

                // Language
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val idx = (safeLocaleIndex - 1 + locales.size) % locales.size
                                viewModel.updateLanguage(locales[idx].toLanguageTag())
                            },
                            enabled = locales.isNotEmpty()
                        ) { Text("◀", modifier = Modifier.semantics { contentDescription = "Previous language" }) }
                        Button(
                            onClick = {
                                val idx = (safeLocaleIndex + 1) % locales.size
                                viewModel.updateLanguage(locales[idx].toLanguageTag())
                            },
                            enabled = locales.isNotEmpty()
                        ) { Text("▶", modifier = Modifier.semantics { contentDescription = "Next language" }) }
                    }
                }

                // Voice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val voiceLabel = currentVoice?.let { "${it.name} (${if (viewModel.isOnline(it)) "Online" else "Offline"})" } ?: "No voices"
                    Text(
                        text = "Voice: $voiceLabel",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (voices.isNotEmpty()) {
                                    val idx = (safeVoiceIndex - 1 + voices.size) % voices.size
                                    viewModel.updateVoice(voices[idx].name)
                                }
                            },
                            enabled = voices.isNotEmpty()
                        ) { Text("◀", modifier = Modifier.semantics { contentDescription = "Previous voice" }) }
                        Button(
                            onClick = {
                                if (voices.isNotEmpty()) {
                                    val idx = (safeVoiceIndex + 1) % voices.size
                                    viewModel.updateVoice(voices[idx].name)
                                }
                            },
                            enabled = voices.isNotEmpty()
                        ) { Text("▶", modifier = Modifier.semantics { contentDescription = "Next voice" }) }
                    }
                }
            }

            // ✅ 4. SPEECH RATE SLIDER with Reset Button
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentSpeechRate = settings?.speechRate ?: 1.0f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speech Rate: ${String.format("%.1f", currentSpeechRate)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { viewModel.updateSpeechRate(1.0f) },
                        enabled = currentSpeechRate != 1.0f,
                        modifier = Modifier.semantics { contentDescription = "Reset speech rate to default" }
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Slider(
                    value = settings?.speechRate ?: 1.0f,
                    onValueChange = { viewModel.updateSpeechRate(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Adjust speech rate" }
                )
            }

            // ✅ 5. PITCH SLIDER with Reset Button
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentPitch = settings?.pitch ?: 1.0f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pitch: ${String.format("%.1f", currentPitch)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { viewModel.updatePitch(1.0f) },
                        enabled = currentPitch != 1.0f,
                        modifier = Modifier.semantics { contentDescription = "Reset pitch to default" }
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Slider(
                    value = settings?.pitch ?: 1.0f,
                    onValueChange = { viewModel.updatePitch(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Adjust pitch" }
                )
            }

            // ✅ 6. READ EXACT SELECTION SWITCH
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

            // ✅ 7. OCR MODE SWITCH
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

            // ✅ 8. FLOATING BUTTON SIZE SLIDER with Reset Button
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentButtonSize = settings?.floatingButtonSize ?: 1.0f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Floating Button Size: ${String.format("%.1f", currentButtonSize)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { viewModel.updateFloatingButtonSize(1.0f) },
                        enabled = currentButtonSize != 1.0f,
                        modifier = Modifier.semantics { contentDescription = "Reset floating button size to default" }
                    ) {
                        Text("Reset", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Slider(
                    value = settings?.floatingButtonSize ?: 1.0f,
                    onValueChange = { viewModel.updateFloatingButtonSize(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Adjust floating button size" }
                )
            }
        }
    }
}