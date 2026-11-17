package com.debk007.speakfast.speech

import android.content.Context
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

    private val _localesState = MutableStateFlow<List<Locale>>(emptyList())
    val localesState: StateFlow<List<Locale>> = _localesState.asStateFlow()

    // FIX (stale play/pause state): emits true whenever speech stops
    // (natural end, pause, stop, or TTS error) so UIs can reset their state.
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
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                _localesState.value = (textToSpeech?.availableLanguages ?: emptyList())
                    .distinctBy { it.toLanguageTag() }
                    .sortedBy { it.displayLanguage }
                updateSpeechSettings()
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
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
                })
                Log.d("SpeechManager", "TTS initialized successfully")
            } else {
                Log.e("SpeechManager", "TTS init failed: $status")
            }
        }
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
            initialize(); 
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