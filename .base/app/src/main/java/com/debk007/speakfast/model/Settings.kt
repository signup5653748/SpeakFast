package com.debk007.speakfast.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val languageCode: String = "en-US",
    val voice: String = "",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val ocrEnabled: Boolean = false,
    val readExactSelection: Boolean = true,
    val triggerMethod: TriggerMethod = TriggerMethod.QUICK_SETTINGS_TILE
)

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
