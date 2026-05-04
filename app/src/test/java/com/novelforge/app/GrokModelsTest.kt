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
    }
    
    @Test
    fun `GrokRequest serializes correctly`() {
        val request = GrokRequest(
            model = "grok-4.3",
            messages = listOf(
                Message(role = "user", content = "Hello"),
                Message(role = "assistant", content = "Hi there!")
            ),
            stream = true,
            temperature = 0.8,
            maxTokens = 4096
        )
        
        val jsonString = json.encodeToString(GrokRequest.serializer(), request)
        
        assertTrue(jsonString.contains("grok-4.3"))
        assertTrue(jsonString.contains("user"))
        assertTrue(jsonString.contains("Hello"))
        assertTrue(jsonString.contains("\"stream\":true"))
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
        assertEquals(1, response.choices?.size)
        assertEquals("assistant", response.choices?.first()?.message?.role)
        assertEquals("Test response", response.choices?.first()?.message?.content)
        assertEquals(10, response.usage?.promptTokens)
        assertEquals(20, response.usage?.completionTokens)
        assertEquals(30, response.usage?.totalTokens)
    }
    
    @Test
    fun `Message serializes with role and content`() {
        val message = Message(role = "system", content = "You are a helpful assistant")
        
        val jsonString = json.encodeToString(Message.serializer(), message)
        
        assertTrue(jsonString.contains("\"role\":\"system\""))
        assertTrue(jsonString.contains("helpful assistant"))
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
        assertEquals("invalid_request_error", response.error?.type)
        assertEquals("invalid_api_key", response.error?.code)
    }
}
