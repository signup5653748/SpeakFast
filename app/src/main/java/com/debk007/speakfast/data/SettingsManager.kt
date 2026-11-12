package com.debk007.speakfast.data

import android.content.Context
import com.debk007.speakfast.model.Settings
import com.debk007.speakfast.model.TriggerMethod
import com.debk007.speakfast.model.Language
import com.debk007.speakfast.model.Voice
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File

class SettingsManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val settingsFile = File(context.filesDir, "speakfast_settings.json")

    fun getSettings(): Settings {
        return try {
            if (settingsFile.exists()) {
                val jsonString = settingsFile.readText()
                json.decodeFromString<Settings>(jsonString)
            } else {
                Settings()
            }
        } catch (e: SerializationException) {
            // If there's an error parsing, return default settings
            Settings()
        } catch (e: Exception) {
            Settings()
        }
    }

    fun saveSettings(settings: Settings) {
        try {
            val jsonString = json.encodeToString(Settings::class, settings)
            settingsFile.writeText(jsonString)
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }

    fun updateLanguage(languageCode: String) {
        val settings = getSettings()
        val newSettings = settings.copy(languageCode = languageCode)
        saveSettings(newSettings)
    }

    fun updateVoice(voice: String) {
        val settings = getSettings()
        val newSettings = settings.copy(voice = voice)
        saveSettings(newSettings)
    }

    fun updateSpeechRate(rate: Float) {
        val settings = getSettings()
        val newSettings = settings.copy(speechRate = rate)
        saveSettings(newSettings)
    }

    fun updatePitch(pitch: Float) {
        val settings = getSettings()
        val newSettings = settings.copy(pitch = pitch)
        saveSettings(newSettings)
    }

    fun updateTriggerMethod(triggerMethod: TriggerMethod) {
        val settings = getSettings()
        val newSettings = settings.copy(triggerMethod = triggerMethod)
        saveSettings(newSettings)
    }

    fun resetToDefaults() {
        saveSettings(Settings())
    }
}