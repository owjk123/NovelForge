package com.novelforge.app.data.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.novelforge.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val API_BASE_URL = stringPreferencesKey("api_base_url")
        private val SELECTED_ENDPOINT = stringPreferencesKey("selected_endpoint")
        private val MODEL_NAME = stringPreferencesKey("model_name")
        private val CUSTOM_ENDPOINT_URL = stringPreferencesKey("custom_endpoint_url")
        private val CUSTOM_MODEL_NAME = stringPreferencesKey("custom_model_name")

        const val ENDPOINT_APIYI = "apiyi"
        const val ENDPOINT_T8STAR = "t8star"
        const val ENDPOINT_OPENAI = "openai"
        const val ENDPOINT_CUSTOM = "custom"

        val ENDPOINT_URLS = mapOf(
            ENDPOINT_APIYI to "https://api.apiyi.com/v1",
            ENDPOINT_T8STAR to "https://ai.t8star.cn/v1",
            ENDPOINT_OPENAI to "https://api.openai.com/v1",
            ENDPOINT_CUSTOM to ""
        )

        val PRESET_MODELS = listOf(
            "grok-4.3",
            "grok-4.20",
            "gpt-4o",
            "gpt-4o-mini",
            "claude-sonnet-4-20250514",
            "deepseek-chat",
            "custom"
        )

        const val DEFAULT_MODEL = "grok-4.3"
        const val DEFAULT_ENDPOINT = ENDPOINT_APIYI
    }

    private val defaultApiKey: String
        get() = BuildConfig.API_KEY.ifBlank { "" }
    
    private val defaultBaseUrl: String
        get() = BuildConfig.API_BASE_URL.ifBlank { ENDPOINT_URLS[DEFAULT_ENDPOINT] ?: "" }

    // ---- Flow-based access ----

    val apiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[API_KEY] ?: defaultApiKey
    }

    val selectedEndpoint: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_ENDPOINT] ?: DEFAULT_ENDPOINT
    }

    val apiBaseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[API_BASE_URL] ?: defaultBaseUrl
    }

    val modelName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MODEL_NAME] ?: DEFAULT_MODEL
    }

    val customEndpointUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_ENDPOINT_URL] ?: ""
    }

    val customModelName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_MODEL_NAME] ?: ""
    }

    // ---- Cached sync access (no runBlocking) ----

    @Volatile private var cachedApiKey: String? = null
    @Volatile private var cachedEndpointUrl: String? = null
    @Volatile private var cachedModelName: String? = null

    fun getApiKeySync(): String {
        return cachedApiKey ?: run {
            // First call: block once to warm cache (only during app startup)
            val value = kotlinx.coroutines.runBlocking { apiKey.first() }
            cachedApiKey = value
            value
        }
    }

    fun getCurrentEndpointUrlSync(): String {
        return cachedEndpointUrl ?: run {
            val endpoint = kotlinx.coroutines.runBlocking { selectedEndpoint.first() }
            val url = if (endpoint == ENDPOINT_CUSTOM) {
                kotlinx.coroutines.runBlocking { customEndpointUrl.first() }.ifBlank { defaultBaseUrl }
            } else {
                ENDPOINT_URLS[endpoint] ?: defaultBaseUrl
            }
            cachedEndpointUrl = url
            url
        }
    }

    fun getCurrentModelSync(): String {
        return cachedModelName ?: run {
            val model = kotlinx.coroutines.runBlocking { modelName.first() }
            val value = if (model == "custom") {
                kotlinx.coroutines.runBlocking { customModelName.first() }.ifBlank { DEFAULT_MODEL }
            } else {
                model
            }
            cachedModelName = value
            value
        }
    }

    private fun invalidateCache() {
        cachedApiKey = null
        cachedEndpointUrl = null
        cachedModelName = null
    }

    // ---- Save methods ----

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
        cachedApiKey = apiKey
    }

    suspend fun saveSelectedEndpoint(endpoint: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_ENDPOINT] = endpoint
            val url = if (endpoint == ENDPOINT_CUSTOM) {
                preferences[CUSTOM_ENDPOINT_URL] ?: ""
            } else {
                ENDPOINT_URLS[endpoint] ?: ""
            }
            preferences[API_BASE_URL] = url
        }
        cachedEndpointUrl = if (endpoint == ENDPOINT_CUSTOM) {
            cachedEndpointUrl // keep current custom URL
        } else {
            ENDPOINT_URLS[endpoint] ?: defaultBaseUrl
        }
    }

    suspend fun saveModelName(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[MODEL_NAME] = modelName
        }
        cachedModelName = if (modelName == "custom") cachedModelName else modelName
    }

    suspend fun saveCustomEndpointUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_ENDPOINT_URL] = url
        }
    }

    suspend fun saveCustomModelName(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_MODEL_NAME] = modelName
        }
    }

    suspend fun saveSettings(
        apiKey: String,
        selectedEndpoint: String,
        modelName: String,
        customEndpointUrl: String = "",
        customModelName: String = ""
    ) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
            preferences[SELECTED_ENDPOINT] = selectedEndpoint
            val baseUrl = if (selectedEndpoint == ENDPOINT_CUSTOM) {
                customEndpointUrl
            } else {
                ENDPOINT_URLS[selectedEndpoint] ?: ""
            }
            preferences[API_BASE_URL] = baseUrl
            preferences[MODEL_NAME] = modelName
            preferences[CUSTOM_ENDPOINT_URL] = customEndpointUrl
            preferences[CUSTOM_MODEL_NAME] = customModelName
        }
        // Update cache immediately
        cachedApiKey = apiKey
        cachedEndpointUrl = if (selectedEndpoint == ENDPOINT_CUSTOM) {
            customEndpointUrl.ifBlank { defaultBaseUrl }
        } else {
            ENDPOINT_URLS[selectedEndpoint] ?: defaultBaseUrl
        }
        cachedModelName = if (modelName == "custom") {
            customModelName.ifBlank { DEFAULT_MODEL }
        } else {
            modelName
        }
    }

    fun getEndpointDisplayName(endpoint: String): String {
        return when (endpoint) {
            ENDPOINT_APIYI -> "API易 (api.apiyi.com)"
            ENDPOINT_T8STAR -> "T8Star (ai.t8star.cn)"
            ENDPOINT_OPENAI -> "官方 OpenAI"
            ENDPOINT_CUSTOM -> "自定义端点"
            else -> endpoint
        }
    }
}
