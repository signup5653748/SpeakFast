package com.debk007.speakfast.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.model.Language
import com.debk007.speakfast.model.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class SpeechManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var currentLocale: Locale = Locale.US

    fun initialize() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                updateSpeechSettings()
                isInitialized = true
                Log.d("SpeakFast", "TextToSpeech initialized successfully")
            } else {
                Log.e("SpeakFast", "TextToSpeech initialization failed: $status")
            }
        }
    }

    fun speakText(text: String) {
        if (!isInitialized || textToSpeech == null) {
            Log.w("SpeakFast", "TextToSpeech not initialized, attempting to initialize")
            initialize()
            return
        }

        if (text.isBlank()) {
            Log.w("SpeakFast", "Attempted to speak empty text")
            return
        }

        textToSpeech?.speak(
            text,
            TextToSpeech.QUEUE_ADD,
            null,
            "SpeakFastUtterance"
        )

        Log.d("SpeakFast", "Speaking text: $text")
    }

    private fun updateSpeechSettings() {
        val settings = settingsManager.getSettings()

        // Set language
        val language = Language.fromCode(settings.languageCode)
        currentLocale = Locale(language.localeCode)

        val langResult = textToSpeech?.setLanguage(currentLocale)
        when (langResult) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                Log.w("SpeakFast", "Language ${language.localeCode} not supported, falling back to US English")
                textToSpeech?.setLanguage(Locale.US)
            }
            else -> {
                Log.d("SpeakFast", "Language set to ${language.localeCode}")
            }
        }

        // Set speech rate
        textToSpeech?.setSpeechRate(settings.speechRate)

        // Set pitch
        textToSpeech?.setPitch(settings.pitch)
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        Log.d("SpeakFast", "TextToSpeech shutdown")
    }

    companion object {
        private const val TAG = "SpeakFast"
    }
}