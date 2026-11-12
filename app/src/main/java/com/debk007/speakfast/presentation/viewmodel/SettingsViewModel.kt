package com.debk007.speakfast.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.model.Settings
import com.debk007.speakfast.model.Language
import com.debk007.speakfast.model.Voice
import com.debk007.speakfast.model.TriggerMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = settingsManager.getSettings()
            _uiState.value = SettingsUiState(
                selectedLanguage = settings.languageCode,
                selectedVoice = settings.voice,
                speechRate = settings.speechRate,
                pitch = settings.pitch,
                triggerMethod = settings.triggerMethod
            )
        }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsManager.updateLanguage(languageCode)
            _uiState.value = _uiState.value.copy(selectedLanguage = languageCode)
        }
    }

    fun updateVoice(voice: String) {
        viewModelScope.launch {
            settingsManager.updateVoice(voice)
            _uiState.value = _uiState.value.copy(selectedVoice = voice)
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch {
            settingsManager.updateSpeechRate(rate)
            _uiState.value = _uiState.value.copy(speechRate = rate)
        }
    }

    fun updatePitch(pitch: Float) {
        viewModelScope.launch {
            settingsManager.updatePitch(pitch)
            _uiState.value = _uiState.value.copy(pitch = pitch)
        }
    }

    fun updateTriggerMethod(triggerMethod: TriggerMethod) {
        viewModelScope.launch {
            settingsManager.updateTriggerMethod(triggerMethod)
            _uiState.value = _uiState.value.copy(triggerMethod = triggerMethod)
        }
    }
}

data class SettingsUiState(
    val selectedLanguage: String = "en",
    val selectedVoice: String = "default",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val triggerMethod: TriggerMethod = TriggerMethod.QUICK_SETTINGS_TILE
)