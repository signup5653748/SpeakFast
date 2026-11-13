package com.debk007.speakfast.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.model.Settings
import com.debk007.speakfast.model.TriggerMethod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val settings: Settings? = null,
    val errorMessage: String? = null
)

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
            _uiState.value = _uiState.value.copy(isLoading = true)
            settingsManager.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    settings = settings,
                    errorMessage = null
                )
            }
        }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = settingsManager.updateLanguage(languageCode)) {
                is SettingsManager.SettingsResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is SettingsManager.SettingsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun updateVoice(voice: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = settingsManager.updateVoice(voice)) {
                is SettingsManager.SettingsResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is SettingsManager.SettingsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = settingsManager.updateSpeechRate(rate)) {
                is SettingsManager.SettingsResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is SettingsManager.SettingsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun updatePitch(pitch: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = settingsManager.updatePitch(pitch)) {
                is SettingsManager.SettingsResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is SettingsManager.SettingsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun updateTriggerMethod(triggerMethod: TriggerMethod) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = settingsManager.updateTriggerMethod(triggerMethod)) {
                is SettingsManager.SettingsResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                is SettingsManager.SettingsResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}