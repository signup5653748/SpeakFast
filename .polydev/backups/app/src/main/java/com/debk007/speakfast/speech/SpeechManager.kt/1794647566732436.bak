package com.debk007.speakfast.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.model.Language
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class SpeechManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                updateSpeechSettings()
                isInitialized = true
            } else {
                Log.e("SpeakFast", "TTS init failed: $status")
            }
        }
    }

    fun speakText(text: String) {
        if (!isInitialized || textToSpeech == null) { initialize(); return }
        if (text.isBlank()) return
        textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, "SpeakFastUtterance")
    }

    fun stop() {
        textToSpeech?.stop()
    }

    private fun updateSpeechSettings() {
        val settings = settingsManager.getSettings()
        val locale = Locale(Language.fromCode(settings.languageCode).localeCode)
        when (textToSpeech?.setLanguage(locale)) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> textToSpeech?.setLanguage(Locale.US)
            else -> {}
        }
        textToSpeech?.setSpeechRate(settings.speechRate)
        textToSpeech?.setPitch(settings.pitch)
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
}