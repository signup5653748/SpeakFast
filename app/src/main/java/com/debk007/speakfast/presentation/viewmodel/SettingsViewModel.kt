package com.debk007.speakfast.presentation.viewmodel

import android.speech.tts.Voice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.model.Settings
import com.debk007.speakfast.model.TriggerMethod
import com.debk007.speakfast.speech.SpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val speechManager: SpeechManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState

    init {
        viewModelScope.launch {
            settingsManager.settingsFlow
                .distinctUntilChanged()
                .collect { settings ->
                    _uiState.update { it.copy(settings = settings) }
                }
        }
        viewModelScope.launch {
            speechManager.localesState
                .distinctUntilChanged()
                .collect { locales ->
                    _uiState.update { it.copy(availableLocales = locales) }
                }
        }
    }

    fun updateLanguage(languageCode: String) {
        val current = _uiState.value.settings ?: return
        settingsManager.updateSettings(current.copy(languageCode = languageCode))
    }

    fun updateVoice(voiceName: String) {
        val current = _uiState.value.settings ?: return
        settingsManager.updateSettings(current.copy(voice = voiceName))
    }

    fun updateSpeechRate(rate: Float) {
        val current = _uiState.value.settings ?: return
        settingsManager.updateSettings(current.copy(speechRate = rate))
    }

    fun updatePitch(pitch: Float) {
        val current = _uiState.value.settings ?: return
        settingsManager.updateSettings(current.copy(pitch = pitch))
    }

    fun updateTriggerMethod(method: TriggerMethod) {
        val current = _uiState.value.settings ?: return
        settingsManager.updateSettings(current.copy(triggerMethod = method))
    }

    fun voicesForSelectedLocale(): List<Voice> {
        val localeCode = _uiState.value.settings?.languageCode ?: "en-US"
        val locale = java.util.Locale.forLanguageTag(localeCode)
        return speechManager.voicesFor(locale)
    }

    fun isOnlineVoice(voice: Voice): Boolean = speechManager.isOnline(voice)

    data class UiState(
        val settings: Settings? = null,
        val availableLocales: List<java.util.Locale> = emptyList()
    )
}