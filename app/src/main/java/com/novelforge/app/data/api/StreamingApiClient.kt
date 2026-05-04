package com.novelforge.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit

class StreamingApiClient {

    private fun getApiService(): GrokApiService {
        return ApiClient.grokApiService
    }
    
    suspend fun generate(request: GrokRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            ApiClient.refreshClient() // Always use fresh client to pick up settings changes
            val response = getApiService().createChatCompletion(request)
            val content = response.choices?.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("No content received"))
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
