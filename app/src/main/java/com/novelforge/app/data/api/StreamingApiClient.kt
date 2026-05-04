package com.novelforge.app.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

sealed class StreamResult {
    data class OnNext(val content: String) : StreamResult()
    data class OnError(val error: String) : StreamResult()
    object OnComplete : StreamResult()
}

class StreamingApiClient {
    
    private val json = ApiClient.getJson()
    private val okHttpClient = ApiClient.getStreamingOkHttpClient()
    private val baseUrl = ApiClient.getBaseUrl()
    
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl + "/")
        .client(okHttpClient.newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    
    private val apiService: GrokApiService = retrofit.create(GrokApiService::class.java)

    private fun createRequest(
        messages: List<Message>,
        stream: Boolean = false,
        temperature: Double = 0.8,
        maxTokens: Int = 4096
    ): GrokRequest {
        return GrokRequest(
            model = ApiClient.getModel(),
            messages = messages,
            stream = stream,
            temperature = temperature,
            maxTokens = maxTokens
        )
    }
    
    suspend fun generate(request: GrokRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createChatCompletion(request)
            val content = response.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("No content received"))
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
