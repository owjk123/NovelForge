package com.novelforge.app.data.api

import retrofit2.http.Body
import retrofit2.http.POST

interface GrokApiService {
    
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body request: GrokRequest
    ): GrokResponse
}
