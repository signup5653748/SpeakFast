[Earlier content for app/src/main/java/com/debk007/speakfast/speech/SpeechManager.kt (9391 chars, already written to disk). The module surface below may not show every signature you need — re-read with read_file before editing this file again, OR before importing/calling anything from it in another file. Do not recreate it from scratch and do not guess a function's parameters from memory.
Module surface:
import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.model.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
class SpeechManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private val sentences = mutableListOf<String>()
    private var currentIndex = 0
    private var isPlaying = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --- State exposed to UI ---
    private val _localesState = MutableStateFlow<List<Locale>>(emptyList())
    val localesState: StateFlow<List<Locale>> = _localesState.asStateFlow()

    private val _enginesState = MutableStateFlow<List<TextToSpeech.EngineInfo>>(emptyList())
    val enginesState: StateFlow<List<TextToSpeech.EngineInfo>> = _enginesState.asStateFlow()

    // Engine switching state
    private val _engineSwitching = MutableStateFlow(false)
    val engineSwitching: StateFlow<Boolean> = _engineSwitching.asStateFlow()

    // Speech progress for highlighting (sentence index, total, char range in utterance)
    data class SpeechProgress(
        val sentenceIndex: Int,
        val totalSentences: Int,
        val charStart: Int,
        val charEnd: Int
    )
    private val _speechProgress = MutableStateFlow<SpeechProgress?>(null)
    val speechProgress: StateFlow<SpeechProgress?> = _speechProgress.asStateFlow()

    // FIX (stale play/pause state): emits true whenever speech stops
    private val _speechFinished = MutableStateFlow(false)
    val speechFinished: StateFlow<Boolean> = _speechFinished.asStateFlow()

    init {
        scope.launch {
            settingsManager.settingsFlow.collect { if (isInitialized) updateSpeechSettings() }
        }
    }

    private fun notifySpeechFinished() {
        _speechFinished.value = true
        scope.launch {
            kotlinx.coroutines.delay(150)
            _speechFinished.value = false
        }
    }

    fun initialize() {
        if (isInitialized) return
        val settings = settingsManager.getSettings()
        val engine = if (settings.ttsEngine.isNotEmpty()) settings.ttsEngine else ""
        textToSpeech = if (engine.isNotEmpty()) {
            TextToSpeech(context, { status -> onInit(status) }, engine)
        } else {
            TextToSpeech(context, { status -> onInit(status) })
        }
        loadEngines()
    }

    private fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            updateLocales()
            loadEngines() // Re-read engines once TTS is truly ready to prevent empty list race condition
            updateSpeechSettings()
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Reset progress at start of new utterance
                    _speechProgress.value = null
                }
                override fun onDone(utteranceId: String?) {
                    if (isPlaying) {
                        currentIndex++
                        if (currentIndex < sentences.size) speakCurrent()
                        else { isPlaying = false; notifySpeechFinished() }
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isPlaying = false
                    notifySpeechFinished()
                }
                // API 26+: word/sentence boundary callbacks for highlighting
                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        _speechProgress.value = SpeechProgress(
                            sentenceIndex = currentIndex,
                            totalSentences = sentences.size,
                            charStart = start,
                            charEnd = end
                        )
                    }
                }
            })
            Log.d("SpeechManager", "TTS initialized successfully")
        } else {
            Log.e("SpeechManager", "TTS init failed: $status")
        }
    }

    private fun loadEngines() {
        val tts = TextToSpeech(context, { })
        val engines = tts.engines
        _enginesState.value = engines
        tts.shutdown()
    }

    fun getEngines(): List<TextToSpeech.EngineInfo> = _enginesState.value

    fun reinitializeWithEngine(engineName: String) {
        val currentEngine = textToSpeech?.engineName
        if (currentEngine == engineName) {
            Log.d("SpeechManager", "Engine $engineName already active, skipping reinit")
            return
        }
        scope.launch {
            _engineSwitching.value = true
            shutdown()
            textToSpeech = TextToSpeech(context, { status -> onInit(status) }, engineName)
            _engineSwitching.value = false
        }
    }

    private fun updateLocales() {
        _localesState.value = (textToSpeech?.availableLanguages ?: emptyList())
            .distinctBy { it.toLanguageTag() }
            .sortedBy { it.displayLanguage }
    }

    fun voicesFor(locale: Locale): List<Voice> =
        (textToSpeech?.voices ?: emptySet())
            .filter { it.locale.toLanguageTag() == locale.toLanguageTag() }
            .sortedBy { it.name }

    fun isOnline(voice: Voice): Boolean = runCatching {
        voice.isNetworkConnectionRequired || (voice.features?.contains("networkTts") == true)
    }.getOrDefault(false)

    fun speakScreen(text: String) {
        if (!isInitialized) {
            Log.w("SpeechManager", "TTS not initialized, initializing now")
            initialize()
            // Wait for initialization then speak
            scope.launch {
                kotlinx.coroutines.delay(500)
                if (isInitialized) speakScreen(text)
            }
            return
        }
        sentences.clear()
        sentences.addAll(text.split(Regex("(?<=[.!?\\n])")).map { it.trim() }.filter { it.isNotEmpty() })
        if (sentences.isEmpty()) sentences.add(text)
        currentIndex = 0
        isPlaying = true
        speakCurrent()
    }

    fun speakText(text: String) = speakScreen(text)

    private fun speakCurrent() {
        if (currentIndex !in sentences.indices) { isPlaying = false; return }
        val utterance = sentences[currentIndex]
        Log.d("SpeechManager", "Speaking: $utterance")
        textToSpeech?.speak(utterance, TextToSpeech.QUEUE_FLUSH, null, "sf_$currentIndex")
    }

    fun pause() {
        if (isPlaying) { isPlaying = false; textToSpeech?.stop(); notifySpeechFinished() }
    }
    fun resume() { if (sentences.isNotEmpty()) { isPlaying = true; speakCurrent() } }
    fun rewind() { if (sentences.isNotEmpty()) { currentIndex = (currentIndex - 1).coerceAtLeast(0); isPlaying = true; speakCurrent() } }
    fun forward() { if (sentences.isNotEmpty()) { currentIndex = (currentIndex + 1).coerceAtMost(sentences.size - 1); isPlaying = true; speakCurrent() } }
    fun stop() {
        if (isPlaying || sentences.isNotEmpty()) notifySpeechFinished()
        isPlaying = false; sentences.clear(); currentIndex = 0; textToSpeech?.stop()
        _speechProgress.value = null
    }
    fun refreshSettings() { if (isInitialized) updateSpeechSettings() }

    private fun updateSpeechSettings() {
        val tts = textToSpeech ?: return
        val settings = settingsManager.getSettings()
        val locale = Locale.forLanguageTag(settings.languageCode)
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.US)
        }
        val saved = tts.voices?.firstOrNull { it.name == settings.voice }
        val fallback = voicesFor(locale).firstOrNull { !isOnline(it) } ?: voicesFor(locale).firstOrNull() ?: tts.defaultVoice
        val target = if (saved != null && saved.locale.toLanguageTag() == locale.toLanguageTag()) saved else fallback
        target?.let { runCatching { tts.voice = it } }
        tts.setSpeechRate(settings.speechRate)
        tts.setPitch(settings.pitch)
    }

    fun shutdown() { stop(); textToSpeech?.shutdown(); textToSpeech = null; isInitialized = false }
}