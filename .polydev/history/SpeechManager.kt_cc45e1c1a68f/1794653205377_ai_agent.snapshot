package com.debk007.speakfast.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.model.Language
import com.debk007.speakfast.model.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    init {
        scope.launch {
            settingsManager.settingsFlow.collect { if (isInitialized) updateSpeechSettings() }
        }
    }

    fun initialize() {
        if (isInitialized) return
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                updateSpeechSettings()
                isInitialized = true
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (isPlaying) {
                            currentIndex++
                            if (currentIndex < sentences.size) speakCurrent() else isPlaying = false
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { isPlaying = false }
                })
            } else {
                Log.e("SpeakFast", "TTS init failed: $status")
            }
        }
    }

    fun speakScreen(text: String) {
        if (!isInitialized) { initialize(); return }
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
        textToSpeech?.speak(sentences[currentIndex], TextToSpeech.QUEUE_FLUSH, null, "sf_$currentIndex")
    }

    fun pause() { isPlaying = false; textToSpeech?.stop() }
    fun resume() { if (sentences.isNotEmpty()) { isPlaying = true; speakCurrent() } }
    fun rewind() { if (sentences.isNotEmpty()) { currentIndex = (currentIndex - 1).coerceAtLeast(0); isPlaying = true; speakCurrent() } }
    fun forward() { if (sentences.isNotEmpty()) { currentIndex = (currentIndex + 1).coerceAtMost(sentences.size - 1); isPlaying = true; speakCurrent() } }
    fun stop() { isPlaying = false; sentences.clear(); currentIndex = 0; textToSpeech?.stop() }
    fun refreshSettings() { if (isInitialized) updateSpeechSettings() }

    private fun updateSpeechSettings() {
        val settings = settingsManager.getSettings()
        val locale = Locale(Language.fromCode(settings.languageCode).localeCode)
        when (textToSpeech?.setLanguage(locale)) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> textToSpeech?.setLanguage(Locale.US)
            else -> {}
        }
        applyVoiceVariant(settings)
    }

    private fun applyVoiceVariant(settings: Settings) {
        val tts = textToSpeech ?: return
        val lang = Locale(Language.fromCode(settings.languageCode).localeCode).language
        val localeVoices = tts.voices?.filter { it.locale.language == lang } ?: emptyList()
        val picked = when (settings.voice) {
            "male" -> localeVoices.firstOrNull { it.name.contains("male", true) || it.name.contains("man", true) }
            "female" -> localeVoices.firstOrNull { it.name.contains("female", true) || it.name.contains("woman", true) }
            else -> null
        } ?: tts.defaultVoice ?: localeVoices.firstOrNull()
        picked?.let { runCatching { tts.voice = it } }
        val pitchMul = when (settings.voice) { "child" -> 1.4f; "elder" -> 0.85f; else -> 1.0f }
        val rateMul = when (settings.voice) { "child" -> 1.05f; "elder" -> 0.9f; else -> 1.0f }
        tts.setPitch(settings.pitch * pitchMul)
        tts.setSpeechRate(settings.speechRate * rateMul)
    }

    fun shutdown() { stop(); textToSpeech?.shutdown(); textToSpeech = null; isInitialized = false }
}