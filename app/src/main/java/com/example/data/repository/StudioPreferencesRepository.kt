package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bharat_storyboard_studio_prefs"
)

data class StudioPreferences(
    val selectedModelId: String = "gemini-3.8-flash",
    val defaultLanguage: String = "Hindi",
    val defaultArtStyle: String = "Anime",
    val defaultAspectRatio: String = "9:16"
)

class StudioPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.studioDataStore

    private object Keys {
        val SELECTED_MODEL = stringPreferencesKey("selected_model_id")
        val DEFAULT_LANGUAGE = stringPreferencesKey("default_language")
        val DEFAULT_ART_STYLE = stringPreferencesKey("default_art_style")
        val DEFAULT_ASPECT_RATIO = stringPreferencesKey("default_aspect_ratio")
    }

    val preferencesFlow: Flow<StudioPreferences> = dataStore.data.map { prefs ->
        StudioPreferences(
            selectedModelId = prefs[Keys.SELECTED_MODEL] ?: "gemini-3.8-flash",
            defaultLanguage = prefs[Keys.DEFAULT_LANGUAGE] ?: "Hindi",
            defaultArtStyle = prefs[Keys.DEFAULT_ART_STYLE] ?: "Anime",
            defaultAspectRatio = prefs[Keys.DEFAULT_ASPECT_RATIO] ?: "9:16"
        )
    }

    suspend fun setSelectedModel(modelId: String) {
        dataStore.edit { it[Keys.SELECTED_MODEL] = modelId }
    }

    suspend fun setDefaultLanguage(language: String) {
        dataStore.edit { it[Keys.DEFAULT_LANGUAGE] = language }
    }

    suspend fun setDefaultArtStyle(style: String) {
        dataStore.edit { it[Keys.DEFAULT_ART_STYLE] = style }
    }

    suspend fun setDefaultAspectRatio(ratio: String) {
        dataStore.edit { it[Keys.DEFAULT_ASPECT_RATIO] = ratio }
    }
}
