package com.debk007.speakfast.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.debk007.speakfast.model.Settings
import com.debk007.speakfast.model.TriggerMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "speakfast_settings")

class SettingsManager(private val context: Context) {

    companion object {
        private const val TAG = "SettingsManager"
        val LANGUAGE_CODE_KEY = stringPreferencesKey("language_code")
        val VOICE_KEY = stringPreferencesKey("voice")
        val SPEECH_RATE_KEY = floatPreferencesKey("speech_rate")
        val PITCH_KEY = floatPreferencesKey("pitch")
        val OCR_ENABLED_KEY = booleanPreferencesKey("ocr_enabled")
        val READ_EXACT_SELECTION_KEY = booleanPreferencesKey("read_exact_selection")
        val TRIGGER_METHOD_KEY = stringPreferencesKey("trigger_method")
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    fun getSettings(): Settings = runBlocking { settingsFlow.first() }

    private fun Preferences.toSettings(): Settings {
        return Settings(
            languageCode = this[LANGUAGE_CODE_KEY] ?: "en-US",
            voice = this[VOICE_KEY] ?: "",
            speechRate = this[SPEECH_RATE_KEY] ?: 1.0f,
            pitch = this[PITCH_KEY] ?: 1.0f,
            ocrEnabled = this[OCR_ENABLED_KEY] ?: false,
            readExactSelection = this[READ_EXACT_SELECTION_KEY] ?: true,
            triggerMethod = TriggerMethod.fromString(this[TRIGGER_METHOD_KEY] ?: "quick_settings_tile")
        )
    }

    suspend fun updateLanguage(languageCode: String): Settings {
        context.dataStore.edit { it[LANGUAGE_CODE_KEY] = languageCode }
        return getSettings()
    }

    suspend fun updateVoice(voice: String): Settings {
        context.dataStore.edit { it[VOICE_KEY] = voice }
        return getSettings()
    }

    suspend fun updateSpeechRate(rate: Float): Settings {
        context.dataStore.edit { it[SPEECH_RATE_KEY] = rate }
        return getSettings()
    }

    suspend fun updatePitch(pitch: Float): Settings {
        context.dataStore.edit { it[PITCH_KEY] = pitch }
        return getSettings()
    }

    suspend fun updateOcrEnabled(enabled: Boolean): Settings {
        context.dataStore.edit { it[OCR_ENABLED_KEY] = enabled }
        return getSettings()
    }

    suspend fun updateReadExactSelection(enabled: Boolean): Settings {
        context.dataStore.edit { it[READ_EXACT_SELECTION_KEY] = enabled }
        return getSettings()
    }

    suspend fun updateTriggerMethod(triggerMethod: TriggerMethod): Settings {
        context.dataStore.edit { it[TRIGGER_METHOD_KEY] = triggerMethod.name.lowercase() }
        return getSettings()
    }
}
