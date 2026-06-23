package com.levelup.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "levelup_prefs")

/**
 * Stores the small handful of user preferences the app needs:
 * the optional AI provider + API key for quest suggestions.
 *
 * Everything else (missions, XP, etc.) lives in the Room database.
 */
class UserPreferences(private val context: Context) {

    private object Keys {
        val aiProvider: Preferences.Key<String> = stringPreferencesKey("ai_provider")
        val aiApiKey: Preferences.Key<String> = stringPreferencesKey("ai_api_key")
        val aiModel: Preferences.Key<String> = stringPreferencesKey("ai_model")
    }

    val aiProvider: Flow<String> = context.dataStore.data
        .map { it[Keys.aiProvider] ?: "offline" }

    val aiApiKey: Flow<String> = context.dataStore.data
        .map { it[Keys.aiApiKey] ?: "" }

    val aiModel: Flow<String> = context.dataStore.data
        .map { it[Keys.aiModel] ?: "gpt-4o-mini" }

    suspend fun setAiProvider(value: String) {
        context.dataStore.edit { it[Keys.aiProvider] = value }
    }

    suspend fun setAiApiKey(value: String) {
        context.dataStore.edit { it[Keys.aiApiKey] = value }
    }

    suspend fun setAiModel(value: String) {
        context.dataStore.edit { it[Keys.aiModel] = value }
    }
}
