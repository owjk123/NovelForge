package com.novelforge.app

import com.novelforge.app.data.api.GrokRequest
import com.novelforge.app.data.api.GrokResponse
import com.novelforge.app.data.api.Message
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.*

class GrokModelsTest {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    @Test
    fun `GrokRequest can be serialized and deserialized`() {
        val request = GrokRequest(
            model = "grok-4.3",
            messages = listOf(
                Message(role = "user", content = "Hello")
            )
        )
        val jsonString = json.encodeToString(GrokRequest.serializer(), request)
        val decoded = json.decodeFromString(GrokRequest.serializer(), jsonString)
        assertEquals("grok-4.3", decoded.model)
        assertEquals(1, decoded.messages.size)
        assertEquals("user", decoded.messages[0].role)
        assertEquals("Hello", decoded.messages[0].content)
    }
    
    @Test
    fun `GrokResponse deserializes correctly`() {
        val responseJson = """
            {
                "id": "chatcmpl-123",
                "object": "chat.completion",
                "created": 1677652288,
                "model": "grok-4.3",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "Test response"
                    },
                    "finish_reason": "stop"
                }],
                "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 20,
                    "total_tokens": 30
                }
            }
        """.trimIndent()
        
        val response = json.decodeFromString(GrokResponse.serializer(), responseJson)
        assertEquals("chatcmpl-123", response.id)
        assertEquals("grok-4.3", response.model)
        assertEquals("Test response", response.choices?.first()?.message?.content)
    }
    
    @Test
    fun `GrokResponse with error deserializes correctly`() {
        val errorJson = """
            {
                "error": {
                    "message": "Invalid API key",
                    "type": "invalid_request_error",
                    "code": "invalid_api_key"
                }
            }
        """.trimIndent()
        
        val response = json.decodeFromString(GrokResponse.serializer(), errorJson)
        assertNotNull(response.error)
        assertEquals("Invalid API key", response.error?.message)
    }
}
