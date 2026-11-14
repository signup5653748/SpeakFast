package com.debk007.speakfast.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val languageCode: String = "en",
    val voice: String = "default",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val triggerMethod: TriggerMethod = TriggerMethod.QUICK_SETTINGS_TILE
)

@Serializable
enum class Language(
    val code: String,
    val displayName: String,
    val localeCode: String
) {
    ENGLISH("en", "English", "en"),
    SPANISH("es", "Spanish", "es"),
    FRENCH("fr", "French", "fr"),
    GERMAN("de", "German", "de"),
    ITALIAN("it", "Italian", "it"),
    PORTUGUESE("pt", "Portuguese", "pt"),
    CHINESE("zh", "Chinese", "zh"),
    JAPANESE("ja", "Japanese", "ja"),
    KOREAN("ko", "Korean", "ko"),
    RUSSIAN("ru", "Russian", "ru"),
    ARABIC("ar", "Arabic", "ar");

    companion object {
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}

@Serializable
enum class Voice(
    val voiceId: String,
    val displayName: String
) {
    DEFAULT("default", "Default"),
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    CHILD("child", "Child"),
    ELDER("elder", "Elder");
}

@Serializable
enum class TriggerMethod {
    QUICK_SETTINGS_TILE,
    FLOATING_BUTTON,
    GESTURE;

    companion object {
        fun fromString(value: String): TriggerMethod {
            return when (value) {
                "floating_button" -> FLOATING_BUTTON
                "gesture" -> GESTURE
                else -> QUICK_SETTINGS_TILE
            }
        }
    }
}