package com.debk007.speakfast.presentation.ui

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debk007.speakfast.presentation.viewmodel.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val locales by viewModel.ttsLocales.collectAsState()
    val engines by viewModel.ttsEngines.collectAsState()
    val settings = uiState.settings
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize().clearAndSetSemantics { },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)

            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    Button(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                }
            }

            val engineItems = if (engines.isEmpty()) listOf(TextToSpeech.EngineInfo().apply { name = ""; label = "System Default" }) else engines
            val currentEngine = engineItems.firstOrNull { it.name == (settings?.ttsEngine ?: "") } ?: engineItems.first()
            SimpleDropdown(
                label = "TTS Engine",
                items = engineItems,
                selected = currentEngine,
                itemText = { it.label },
                onSelected = { viewModel.updateTtsEngine(it.name) }
            )

            if (locales.isEmpty()) {
                Text("Loading TTS languages...", color = MaterialTheme.colorScheme.onSurface)
            } else {
                val safeLocaleIndex = locales.indexOfFirst { it.toLanguageTag() == (settings?.languageCode ?: "") }.coerceAtLeast(0)
                val currentLocale = locales.getOrElse(safeLocaleIndex) { Locale.getDefault() }
                
                SearchableDropdown(
                    label = "Language",
                    items = locales,
                    selected = currentLocale,
                    itemText = { it.displayLanguage },
                    searchFilter = { locale, query -> locale.displayLanguage.contains(query, ignoreCase = true) },
                    onSelected = { viewModel.updateLanguage(it.toLanguageTag()) }
                )

                val voices = viewModel.voicesFor(currentLocale)
                val safeVoiceIndex = voices.indexOfFirst { it.name == (settings?.voice ?: "") }.coerceAtLeast(0)
                val currentVoice = voices.getOrElse(safeVoiceIndex) { null }
                SimpleDropdown(
                    label = "Voice Variant",
                    items = voices,
                    selected = currentVoice,
                    itemText = { v -> "${v.name} (${if (viewModel.isOnline(v)) "Online" else "Offline"})" },
                    onSelected = { viewModel.updateVoice(it.name) }
                )
            }

            // SPEECH RATE SLIDER - Fixed to only save on release
            val speechRateValue = settings?.speechRate ?: 1.0f
            var speechRateLocal by remember(speechRateValue) { mutableFloatStateOf(speechRateValue) }
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Speech Rate: ${String.format("%.1f", speechRateLocal)}", color = MaterialTheme.colorScheme.onSurface)
                    Button(onClick = { 
                        speechRateLocal = 1.0f
                        viewModel.updateSpeechRate(1.0f)
                    }, enabled = speechRateLocal != 1.0f) { Text("Reset") }
                }
                Slider(
                    value = speechRateLocal,
                    onValueChange = { speechRateLocal = it },
                    onValueChangeFinished = { viewModel.updateSpeechRate(speechRateLocal) },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Adjust speech rate" }
                )
            }

            // PITCH SLIDER - Fixed to only save on release
            val pitchValue = settings?.pitch ?: 1.0f
            var pitchLocal by remember(pitchValue) { mutableFloatStateOf(pitchValue) }
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Pitch: ${String.format("%.1f", pitchLocal)}", color = MaterialTheme.colorScheme.onSurface)
                    Button(onClick = { 
                        pitchLocal = 1.0f
                        viewModel.updatePitch(1.0f)
                    }, enabled = pitchLocal != 1.0f) { Text("Reset") }
                }
                Slider(
                    value = pitchLocal,
                    onValueChange = { pitchLocal = it },
                    onValueChangeFinished = { viewModel.updatePitch(pitchLocal) },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Adjust pitch" }
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Read Exact Selection", color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = settings?.readExactSelection ?: true, onCheckedChange = { viewModel.updateReadExactSelection(it) })
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("OCR Mode (Read Images)", color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = settings?.ocrEnabled ?: false, onCheckedChange = { viewModel.updateOcrEnabled(it) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SimpleDropdown(
    label: String, items: List<T>, selected: T?, itemText: (T) -> String, onSelected: (T) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { itemText(it) } ?: "None",
            onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(itemText(item)) }, onClick = { onSelected(item); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SearchableDropdown(
    label: String, items: List<T>, selected: T?, itemText: (T) -> String,
    searchFilter: (T, String) -> Boolean, onSelected: (T) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredItems = if (searchQuery.isBlank()) items else items.filter { searchFilter(it, searchQuery) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { 
        expanded = it
        if (!it) searchQuery = ""
    }) {
        OutlinedTextField(
            value = selected?.let { itemText(it) } ?: "None",
            onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
            filteredItems.forEach { item ->
                DropdownMenuItem(text = { Text(itemText(item)) }, onClick = { onSelected(item); expanded = false })
            }
            if (filteredItems.isEmpty()) {
                DropdownMenuItem(text = { Text("No matches found", color = MaterialTheme.colorScheme.error) }, onClick = {}, enabled = false)
            }
        }
    }
}
