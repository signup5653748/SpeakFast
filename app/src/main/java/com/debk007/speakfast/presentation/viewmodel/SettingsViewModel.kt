package com.debk007.speakfast.presentation.viewmodel

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.model.Settings
import com.debk007.speakfast.model.TriggerMethod
import com.debk007.speakfast.speech.SpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val settings: Settings? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val speechManager: SpeechManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val ttsLocales: StateFlow<List<Locale>> = speechManager.localesState
    val ttsEngines: StateFlow<List<TextToSpeech.EngineInfo>> = speechManager.enginesState

    init {
        // ✅ CRITICAL FIX: Initialize TTS so engines/locales populate even when
        // the Accessibility Service is disabled. Without this, the lists stay empty
        // and the Engine/Language/Voice selectors never appear.
        speechManager.initialize()
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsManager.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(isLoading = false, settings = settings, errorMessage = null)
            }
        }
    }

    fun voicesFor(locale: Locale): List<Voice> = speechManager.voicesFor(locale)
    fun isOnline(voice: Voice): Boolean = speechManager.isOnline(voice)

    fun updateLanguage(tag: String) {
        viewModelScope.launch {
            settingsManager.updateLanguage(tag)
            val locale = Locale.forLanguageTag(tag)
            val preferred = speechManager.voicesFor(locale).firstOrNull { !speechManager.isOnline(it) }
                ?: speechManager.voicesFor(locale).firstOrNull()
            preferred?.let { settingsManager.updateVoice(it.name) }
            speechManager.refreshSettings()
        }
    }

    fun updateVoice(name: String) {
        viewModelScope.launch {
            settingsManager.updateVoice(name)
            speechManager.refreshSettings()
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch { settingsManager.updateSpeechRate(rate); speechManager.refreshSettings() }
    }

    fun updatePitch(pitch: Float) {
        viewModelScope.launch { settingsManager.updatePitch(pitch); speechManager.refreshSettings() }
    }

    fun updateOcrEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsManager.updateOcrEnabled(enabled) }
    }

    fun updateReadExactSelection(enabled: Boolean) {
        viewModelScope.launch { settingsManager.updateReadExactSelection(enabled) }
    }

    fun updateTriggerMethod(triggerMethod: TriggerMethod) {
        viewModelScope.launch { settingsManager.updateTriggerMethod(triggerMethod) }
    }

    fun updateTtsEngine(engineName: String) {
        viewModelScope.launch {
            settingsManager.updateTtsEngine(engineName)
            speechManager.reinitializeWithEngine(engineName)
        }
    }

    fun updateFloatingButtonSize(size: Float) {
        viewModelScope.launch { settingsManager.updateFloatingButtonSize(size) }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
}