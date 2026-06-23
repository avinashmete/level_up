@file:OptIn(com.russhwolf.settings.ExperimentalSettingsApi::class)

package com.levelup.app.data.prefs

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cross-platform user preferences backed by multiplatform-settings.
 * Stores the optional AI provider + API key + model for the Quest Forge.
 *
 * We use `getStringOrNullFlow` and apply defaults ourselves so the API surface stays
 * identical across library versions.
 */
class UserPreferences(private val settings: ObservableSettings) {

    private object Keys {
        const val AI_PROVIDER = "ai_provider"
        const val AI_API_KEY = "ai_api_key"
        const val AI_MODEL = "ai_model"
    }

    val aiProvider: Flow<String> = settings.getStringOrNullFlow(Keys.AI_PROVIDER)
        .map { it ?: DEFAULT_PROVIDER }

    val aiApiKey: Flow<String> = settings.getStringOrNullFlow(Keys.AI_API_KEY)
        .map { it ?: "" }

    val aiModel: Flow<String> = settings.getStringOrNullFlow(Keys.AI_MODEL)
        .map { it ?: DEFAULT_MODEL }

    fun setAiProvider(value: String) { settings.putString(Keys.AI_PROVIDER, value) }
    fun setAiApiKey(value: String) { settings.putString(Keys.AI_API_KEY, value) }
    fun setAiModel(value: String) { settings.putString(Keys.AI_MODEL, value) }

    companion object {
        const val DEFAULT_PROVIDER = "offline"
        const val DEFAULT_MODEL = "gpt-4o-mini"
    }
}
