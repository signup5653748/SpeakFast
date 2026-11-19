package com.debk007.speakfast.presentation.ui

import android.content.Context
import android.content.Intent
import android.speech.tts.Voice
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.rememberInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    // FIX (language fallback edge case): if the saved languageCode is not in the
    // TTS locale list, indexOf returns -1. coerceAtLeast(0) maps -1 -> 0 so the
    // ◀/▶ buttons always wrap around the available locales and never index
    // negatively or jump to arbitrary locales.
    val currentLocale = locales.firstOrNull { it.toLanguageTag() == (settings?.languageCode ?: "") }
        ?: Locale.forLanguageTag(settings?.languageCode ?: "en-US")
    val voices: List<Voice> = viewModel.voicesFor(currentLocale)
    val currentVoice = voices.firstOrNull { it.name == (settings?.voice ?: "") } ?: voices.firstOrNull()

    // Dropdown expansion states
    var languageExpanded by remember { mutableStateOf(false) }
    var voiceExpanded by remember { mutableStateOf(false) }

    // Voice search query
    var voiceSearchQuery by remember { mutableStateOf("") }

    // Clear search when language changes
    LaunchedEffect(currentLocale) {
        voiceSearchQuery = ""
    }

    // Filtered voices based on search
    val filteredVoices = remember(voiceSearchQuery, voices) {
        if (voiceSearchQuery.isEmpty()) {
            voices
        } else {
            voices.filter { it.name.contains(voiceSearchQuery, ignoreCase = true) }
        }
    }

    // Interaction sources for ripple effects on icon buttons
    val dismissErrorInteractionSource = remember { MutableInteractionSource() }
    val fontSizeDecreaseInteractionSource = remember { MutableInteractionSource() }
    val fontSizeIncreaseInteractionSource = remember { MutableInteractionSource() }
    val floatingSizeDecreaseInteractionSource = remember { MutableInteractionSource() }
    val floatingSizeIncreaseInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)

        // FIX (error messages): show the ViewModel's errorMessage as a dismissible
        // red banner at the top of the screen.
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
            // LANGUAGE SETTING - Dropdown
            ExposedDropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = currentLocale.displayLanguage,
                    onValueChange = { },
                    label = { Text("Language") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(languageExpanded) }
                )
                locales.forEach { locale ->
                    ExposedDropdownMenuItem(
                        text = { Text(locale.displayLanguage) },
                        onClick = {
                            viewModel.updateLanguage(locale.toLanguageTag())
                            languageExpanded = false
                        },
                        contentDescription = locale.displayLanguage
                    )
                }
            }

            // VOICE SEARCH
            TextField(
                value = voiceSearchQuery,
                onValueChange = { voiceSearchQuery = it },
                label = { Text("Search voices...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (voiceSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { voiceSearchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp).padding(end = 16.dp)
                        )
                    }
                }
            )

            // VOICE VARIANT SETTING - Dropdown
            if (filteredVoices.isEmpty()) {
                Text(
                    text = "No voices matching \"$voiceSearchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ExposedDropdownMenu(
                    expanded = voiceExpanded,
                    onDismissRequest = { voiceExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val voiceLabel = currentVoice?.let {
                        "${it.name} [${if (viewModel.isOnline(it)) "online" else "offline"}]"
                    } ?: "No voices available"
                    TextField(
                        value = voiceLabel,
                        onValueChange = { },
                        label = { Text("Voice Variant") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(voiceExpanded) }
                    )
                    filteredVoices.forEach { voice ->
                        ExposedDropdownMenuItem(
                            text = {
                                Text(
                                    "${voice.name} [${if (viewModel.isOnline(voice)) "online" else "offline"}]"
                                )
                            },
                            onClick = {
                                viewModel.updateVoice(voice.name)
                                voiceExpanded = false
                            },
                            contentDescription = voice.name
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
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = settings?.speechRate ?: 1.0f,
                onValueChange = { viewModel.updateSpeechRate(it) },
                valueRange = 0.5f..2.0f,
                steps = 15,
                modifier = Modifier
                    .fillMaxWidth()
                    .width(200.dp)
                    // Prevent scroll container from stealing touch events from slider
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { /* Consume tap to prevent scroll interference */ },
                            onDoubleTap = { /* Consume double tap */ },
                            onLongPress = { /* Consume long press */ },
                            onPress = { /* Consume press */ }
                        )
                    }
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
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = settings?.pitch ?: 1.0f,
                onValueChange = { viewModel.updatePitch(it) },
                valueRange = 0.5f..2.0f,
                steps = 15,
                modifier = Modifier
                    .fillMaxWidth()
                    .width(200.dp)
                    // Prevent scroll container from stealing touch events from slider
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { /* Consume tap to prevent scroll interference */ },
                            onDoubleTap = { /* Consume double tap */ },
                            onLongPress = { /* Consume long press */ },
                            onPress = { /* Consume press */ }
                        )
                    }
                    .semantics { contentDescription = "Adjust pitch" }
            )
        }

        // FONT SIZE SETTING (replaces pinch zoom - static slider with 10 steps)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Font Size: ${String.format("%.0f", settings?.fontSize ?: 16.0f)}sp",
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth().width(200.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val currentSize = settings?.fontSize ?: 16.0f
                        val newSize = (currentSize - 2f).coerceIn(12f, 30f)
                        viewModel.updateFontSize(newSize)
                    },
                    interactionSource = fontSizeDecreaseInteractionSource,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Decrease font size" }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Decrease font size",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Slider(
                    value = settings?.fontSize ?: 16.0f,
                    onValueChange = { viewModel.updateFontSize(it) },
                    valueRange = 12f..30f,
                    steps = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Prevent scroll container from stealing touch events from slider
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { /* Consume tap to prevent scroll interference */ },
                                onDoubleTap = { /* Consume double tap */ },
                                onLongPress = { /* Consume long press */ },
                                onPress = { /* Consume press */ }
                            )
                        }
                        .semantics { contentDescription = "Adjust font size" }
                )
                IconButton(
                    onClick = {
                        val currentSize = settings?.fontSize ?: 16.0f
                        val newSize = (currentSize + 2f).coerceIn(12f, 30f)
                        viewModel.updateFontSize(newSize)
                    },
                    interactionSource = fontSizeIncreaseInteractionSource,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Increase font size" }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Increase font size",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // FLOATING BUTTON SIZE SETTING - NEW: Central slider with +/- buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Floating Button Size: ${String.format("%.1f", settings?.floatingButtonSize ?: 1.0f)}x",
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                modifier = Modifier.fillMaxWidth().width(200.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val currentSize = settings?.floatingButtonSize ?: 1.0f
                        val newSize = (currentSize - 0.1f).coerceIn(0.5f, 2.0f)
                        viewModel.updateFloatingButtonSize(newSize)
                        // Send broadcast to update floating trigger bubble
                        Intent("com.debk007.speakfast.ACTION_UPDATE_BUBBLE_SIZE").apply {
                            putExtra("size", newSize)
                            setPackage(context.packageName)
                        }.let { context.sendBroadcast(it) }
                    },
                    interactionSource = floatingSizeDecreaseInteractionSource,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Decrease floating button size" }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "Decrease floating button size",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Slider(
                    value = settings?.floatingButtonSize ?: 1.0f,
                    onValueChange = { newSize ->
                        viewModel.updateFloatingButtonSize(newSize)
                        // Send broadcast to update floating trigger bubble
                        Intent("com.debk007.speakfast.ACTION_UPDATE_BUBBLE_SIZE").apply {
                            putExtra("size", newSize)
                            setPackage(context.packageName)
                        }.let { context.sendBroadcast(it) }
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Prevent scroll container from stealing touch events from slider
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { /* Consume tap to prevent scroll interference */ },
                                onDoubleTap = { /* Consume double tap */ },
                                onLongPress = { /* Consume long press */ },
                                onPress = { /* Consume press */ }
                            )
                        }
                        .semantics { contentDescription = "Adjust floating button size" }
                )
                IconButton(
                    onClick = {
                        val currentSize = settings?.floatingButtonSize ?: 1.0f
                        val newSize = (currentSize + 0.1f).coerceIn(0.5f, 2.0f)
                        viewModel.updateFloatingButtonSize(newSize)
                        // Send broadcast to update floating trigger bubble
                        Intent("com.debk007.speakfast.ACTION_UPDATE_BUBBLE_SIZE").apply {
                            putExtra("size", newSize)
                            setPackage(context.packageName)
                        }.let { context.sendBroadcast(it) }
                    },
                    interactionSource = floatingSizeIncreaseInteractionSource,
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Increase floating button size" }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Increase floating button size",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // READ EXACT SELECTION SETTING
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
                    // Ensure minimum 48dp touch target
                    .padding(16.dp)
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
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = settings?.ocrEnabled ?: false,
                onCheckedChange = { viewModel.updateOcrEnabled(it) },
                modifier = Modifier
                    .semantics { contentDescription = "Toggle OCR mode" }
                    // Ensure minimum 48dp touch target
                    .padding(16.dp)
            )
        }
    }
}