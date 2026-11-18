[Earlier content for app/src/main/java/com/debk007/speakfast/presentation/viewmodel/SettingsViewModel.kt (7672 chars, already written to disk). The module surface below may not show every signature you need — re-read with read_file before editing this file again, OR before importing/calling anything from it in another file. Do not recreate it from scratch and do not guess a function's parameters from memory.
Module surface:
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.model.Settings
import com.debk007.speakfast.model.TriggerMethod
import com.debk007.speakfast.speech.SpeechManager
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.ModelDownloader
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

sealed class OcrModelState {
    object NotNeeded : OcrModelState()
    data class Downloading(val progress: Int) : OcrModelState()
    object Downloaded : OcrModelState()
    data class Failed(val error: String) : OcrModelState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val speechManager: SpeechManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ✅ OCR Model Download State
    private val _ocrModelState = MutableStateFlow<OcrModelState>(OcrModelState.NotNeeded)
    val ocrModelState: StateFlow<OcrModelState> = _ocrModelState.asStateFlow()

    val ttsLocales: StateFlow<List<Locale>> = speechManager.localesState
    val ttsEngines: StateFlow<List<TextToSpeech.EngineInfo>> = speechManager.enginesState

    init {
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
        viewModelScope.launch {
            settingsManager.updateOcrEnabled(enabled)
            if (enabled) {
                checkAndDownloadOcrModel()
            }
        }
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

    // ✅ OCR Model Download Logic
    private fun checkAndDownloadOcrModel() {
        _ocrModelState.value = OcrModelState.Downloading(0)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val downloader = ModelDownloader.getInstance(context)
                
                // Check if model is already downloaded
                val model = recognizer.getModelForInference()
                val conditions = DownloadConditions.Builder()
                    .requireWifi()
                    .build()
                
                downloader.getModelDownloadStatus(model)
                    .addOnSuccessListener { status ->
                        if (status == ModelDownloader.MODEL_STATUS_DOWNLOADED) {
                            _ocrModelState.value = OcrModelState.Downloaded
                        } else {
                            // Start download
                            downloader.download(model, conditions)
                                .addOnProgressListener { progress ->
                                    val percent = (progress.downloadedBytes * 100 / progress.totalBytes).toInt()
                                    _ocrModelState.value = OcrModelState.Downloading(percent.coerceIn(0, 100))
                                }
                                .addOnSuccessListener {
                                    _ocrModelState.value = OcrModelState.Downloaded
                                }
                                .addOnFailureListener { e ->
                                    _ocrModelState.value = OcrModelState.Failed(e.message ?: "Download failed")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        _ocrModelState.value = OcrModelState.Failed(e.message ?: "Failed to check model status")
                    }
            } catch (e: Exception) {
                _ocrModelState.value = OcrModelState.Failed(e.message ?: "Unexpected error")
            }
        }
    }

    fun retryOcrModelDownload() {
        checkAndDownloadOcrModel()
    }
}