package com.debk007.speakfast.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.debk007.speakfast.model.Settings
import com.debk007.speakfast.model.TriggerMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "speakfast_settings")

class SettingsManager(private val context: Context) {

    companion object {
        private const val TAG = "SettingsManager"
        val LANGUAGE_CODE_KEY = stringPreferencesKey("language_code")
        val VOICE_KEY = stringPreferencesKey("voice")
        val SPEECH_RATE_KEY = floatPreferencesKey("speech_rate")
        val PITCH_KEY = floatPreferencesKey("pitch")
        val TRIGGER_METHOD_KEY = stringPreferencesKey("trigger_method")
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data
        .map { preferences ->
            preferences.toSettings()
        }

    private fun Preferences.toSettings(): Settings {
        return Settings(
            languageCode = this[LANGUAGE_CODE_KEY] ?: "en",
            voice = this[VOICE_KEY] ?: "default",
            speechRate = this[SPEECH_RATE_KEY] ?: 1.0f,
            pitch = this[PITCH_KEY] ?: 1.0f,
            triggerMethod = TriggerMethod.fromString(this[TRIGGER_METHOD_KEY] ?: "quick_settings_tile")
        )
    }

    sealed class SettingsResult {
        data class Success(val settings: Settings) : SettingsResult()
        data class Error(val message: String, val exception: Exception? = null) : SettingsResult()
    }

    suspend fun saveSettings(newSettings: Settings): SettingsResult {
        return try {
            context.dataStore.edit { preferences ->
                preferences[LANGUAGE_CODE_KEY] = newSettings.languageCode
                preferences[VOICE_KEY] = newSettings.voice
                preferences[SPEECH_RATE_KEY] = newSettings.speechRate
                preferences[PITCH_KEY] = newSettings.pitch
                preferences[TRIGGER_METHOD_KEY] = newSettings.triggerMethod.name.lowercase()
            }
            Log.d(TAG, "Settings saved successfully")
            SettingsResult.Success(newSettings)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings: ${e.message}", e)
            SettingsResult.Error("Failed to save settings: ${e.message}", e)
        }
    }

    suspend fun updateLanguage(languageCode: String): SettingsResult {
        return try {
            context.dataStore.edit { preferences ->
                preferences[LANGUAGE_CODE_KEY] = languageCode
            }
            SettingsResult.Success(Settings(languageCode = languageCode))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update language: ${e.message}", e)
            SettingsResult.Error("Failed to update language", e)
        }
    }

    suspend fun updateVoice(voice: String): SettingsResult {
        return try {
            context.dataStore.edit { preferences ->
                preferences[VOICE_KEY] = voice
            }
            SettingsResult.Success(Settings(voice = voice))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update voice: ${e.message}", e)
            SettingsResult.Error("Failed to update voice", e)
        }
    }

    suspend fun updateSpeechRate(rate: Float): SettingsResult {
        return try {
            context.dataStore.edit { preferences ->
                preferences[SPEECH_RATE_KEY] = rate
            }
            SettingsResult.Success(Settings(speechRate = rate))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update speech rate: ${e.message}", e)
            SettingsResult.Error("Failed to update speech rate", e)
        }
    }

    suspend fun updatePitch(pitch: Float): SettingsResult {
        return try {
            context.dataStore.edit { preferences ->
                preferences[PITCH_KEY] = pitch
            }
            SettingsResult.Success(Settings(pitch = pitch))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update pitch: ${e.message}", e)
            SettingsResult.Error("Failed to update pitch", e)
        }
    }

    suspend fun updateTriggerMethod(triggerMethod: TriggerMethod): SettingsResult {
        return try {
            context.dataStore.edit { preferences ->
                preferences[TRIGGER_METHOD_KEY] = triggerMethod.name.lowercase()
            }
            SettingsResult.Success(Settings(triggerMethod = triggerMethod))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update trigger method: ${e.message}", e)
            SettingsResult.Error("Failed to update trigger method", e)
        }
    }
}