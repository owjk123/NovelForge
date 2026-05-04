package com.novelforge.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
    
    fun streamGenerate(request: GrokRequest): Flow<StreamResult> = flow {
        try {
            val response = apiService.createChatCompletion(request)
            response.choices?.firstOrNull()?.message?.content?.let { content ->
                emit(StreamResult.OnNext(content))
            }
            emit(StreamResult.OnComplete)
        } catch (e: Exception) {
            emit(StreamResult.OnError(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
    
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
