package com.novelforge.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.novelforge.app.data.api.GrokModels
import com.novelforge.app.data.api.GrokRequest
import com.novelforge.app.data.api.Message
import com.novelforge.app.data.preference.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class SettingsUiState(
    val apiKey: String = "",
    val selectedEndpoint: String = SettingsManager.DEFAULT_ENDPOINT,
    val modelName: String = SettingsManager.DEFAULT_MODEL,
    val customEndpointUrl: String = "",
    val customModelName: String = "",
    val isTestingConnection: Boolean = false,
    val testResult: TestResult? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

sealed class TestResult {
    object Success : TestResult()
    data class Error(val message: String) : TestResult()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val presetModels = SettingsManager.PRESET_MODELS
    val endpoints = listOf(
        SettingsManager.ENDPOINT_APIYI,
        SettingsManager.ENDPOINT_T8STAR,
        SettingsManager.ENDPOINT_OPENAI,
        SettingsManager.ENDPOINT_CUSTOM
    )

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                apiKey = settingsManager.apiKey.first(),
                selectedEndpoint = settingsManager.selectedEndpoint.first(),
                modelName = settingsManager.modelName.first(),
                customEndpointUrl = settingsManager.customEndpointUrl.first(),
                customModelName = settingsManager.customModelName.first()
            )
        }
    }

    fun updateApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(apiKey = apiKey, testResult = null)
    }

    fun updateSelectedEndpoint(endpoint: String) {
        _uiState.value = _uiState.value.copy(selectedEndpoint = endpoint, testResult = null)
    }

    fun updateModelName(modelName: String) {
        _uiState.value = _uiState.value.copy(modelName = modelName, testResult = null)
    }

    fun updateCustomEndpointUrl(url: String) {
        _uiState.value = _uiState.value.copy(customEndpointUrl = url, testResult = null)
    }

    fun updateCustomModelName(modelName: String) {
        _uiState.value = _uiState.value.copy(customModelName = modelName, testResult = null)
    }

    fun testConnection() {
        val state = _uiState.value
        if (state.apiKey.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请先输入 API Key")
            return
        }

        val baseUrl = if (state.selectedEndpoint == SettingsManager.ENDPOINT_CUSTOM) {
            state.customEndpointUrl
        } else {
            SettingsManager.ENDPOINT_URLS[state.selectedEndpoint] ?: ""
        }

        if (baseUrl.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请先配置端点 URL")
            return
        }

        val model = if (state.modelName == "custom") {
            if (state.customModelName.isBlank()) {
                _uiState.value = state.copy(errorMessage = "请先输入自定义模型名")
                return
            }
            state.customModelName
        } else {
            state.modelName
        }

        _uiState.value = state.copy(isTestingConnection = true, testResult = null, errorMessage = null)

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val json = Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    }

                    val client = OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val requestBody = GrokRequest(
                        model = model,
                        messages = listOf(
                            Message(role = "user", content = "Hi")
                        ),
                        stream = false,
                        temperature = 0.8,
                        maxTokens = 10
                    )

                    val requestJson = json.encodeToString(GrokModels.GrokRequest.serializer(), requestBody)

                    val request = Request.Builder()
                        .url("$baseUrl/chat/completions")
                        .post(requestJson.toRequestBody("application/json".toMediaType()))
                        .header("Authorization", "Bearer ${state.apiKey}")
                        .header("Content-Type", "application/json")
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful) {
                        TestResult.Success
                    } else {
                        TestResult.Error("连接失败: ${response.code} - ${responseBody ?: "Unknown error"}")
                    }
                } catch (e: Exception) {
                    TestResult.Error("连接失败: ${e.message}")
                }
            }

            _uiState.value = _uiState.value.copy(
                isTestingConnection = false,
                testResult = result
            )
        }
    }

    fun saveSettings() {
        val state = _uiState.value

        if (state.apiKey.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入 API Key")
            return
        }

        if (state.selectedEndpoint == SettingsManager.ENDPOINT_CUSTOM && state.customEndpointUrl.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入自定义端点 URL")
            return
        }

        if (state.modelName == "custom" && state.customModelName.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入自定义模型名")
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessage = null)

        viewModelScope.launch {
            settingsManager.saveSettings(
                apiKey = state.apiKey,
                selectedEndpoint = state.selectedEndpoint,
                modelName = state.modelName,
                customEndpointUrl = state.customEndpointUrl,
                customModelName = state.customModelName
            )
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = true
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun getEndpointDisplayName(endpoint: String): String {
        return settingsManager.getEndpointDisplayName(endpoint)
    }
}
