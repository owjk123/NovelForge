package com.novelforge.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GrokRequest(
    val model: String = "grok-4.3",
    val messages: List<Message>,
    val stream: Boolean = true,
    val temperature: Double = 0.8,
    @SerialName("max_tokens")
    val maxTokens: Int = 4096
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class GrokResponse(
    val id: String? = null,
    val `object`: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null,
    val error: ErrorDetail? = null
)

@Serializable
data class Choice(
    val index: Int? = null,
    val message: Message? = null,
    val delta: Message? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("total_tokens")
    val totalTokens: Int? = null
)

@Serializable
data class ErrorDetail(
    val message: String,
    val type: String? = null,
    val code: String? = null
)
