package com.novelforge.app.domain.usecase

import com.novelforge.app.data.api.ApiClient
import com.novelforge.app.data.api.GrokRequest
import com.novelforge.app.data.api.Message
import com.novelforge.app.data.api.StreamingApiClient
import com.novelforge.app.data.model.Chapter
import com.novelforge.app.data.repository.NovelRepository
import com.novelforge.app.domain.prompt.ChapterGuidance
import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.domain.prompt.NovelPromptBuilder
import com.novelforge.app.domain.prompt.NovelPromptParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class GenerationState {
    data class Generating(val partialContent: String) : GenerationState()
    data class Success(val chapter: Chapter) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

class GenerateChapterUseCase(
    private val repository: NovelRepository,
    private val streamingApiClient: StreamingApiClient
) {
    
    suspend fun generateNewChapter(
        novelId: Long,
        genre: NovelGenre,
        title: String,
        characterSetting: String,
        worldSetting: String,
        chapterSummary: String = "",
        chapterTitle: String = "",
        targetWordCount: Int = 2000,
        chapterGuidance: ChapterGuidance? = null
    ): Flow<GenerationState> {
        return try {
            val previousChapters = repository.getChaptersByNovelIdSync(novelId)
            // 拼接前几章摘要 + 最后一章内容，提供充分上下文
            val previousContent = buildString {
                if (previousChapters.size > 1) {
                    appendLine("【各章摘要】")
                    for (ch in previousChapters.dropLast(1)) {
                        if (ch.summary.isNotBlank()) {
                            appendLine("第${ch.order}章: ${ch.summary}")
                        } else {
                            appendLine("第${ch.order}章: ${ch.content.take(100)}…")
                        }
                    }
                    appendLine()
                }
                previousChapters.lastOrNull()?.let { last ->
                    append(last.content)
                }
            }
            val chapterOrder = repository.getNextChapterOrder(novelId)
            
            val systemPrompt = NovelPromptBuilder.buildSystemPrompt()
            val userPrompt = NovelPromptBuilder.buildUserPrompt(
                NovelPromptParams(
                    genre = genre,
                    title = title,
                    characterSetting = characterSetting,
                    worldSetting = worldSetting,
                    chapterSummary = chapterSummary,
                    previousContent = previousContent,
                    targetWordCount = targetWordCount,
                    isNewChapter = true,
                    chapterTitle = "${chapterOrder}章",
                    chapterGuidance = chapterGuidance
                )
            )
            
            // 从设置中获取当前模型
            val currentModel = ApiClient.getModel()
            
            val request = GrokRequest(
                model = currentModel,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                stream = false,
                temperature = 0.8,
                maxTokens = 4096
            )
            
            flow {
                val result = streamingApiClient.generate(request)
                result.fold(
                    onSuccess = { content ->
                        val chapter = Chapter(
                            novelId = novelId,
                            order = chapterOrder,
                            title = chapterTitle.ifBlank { "第${chapterOrder}章" },
                            content = content,
                            summary = chapterSummary
                        )
                        repository.insertChapter(chapter)
                        emit(GenerationState.Success(chapter))
                    },
                    onFailure = { e ->
                        emit(GenerationState.Error(e.message ?: "Unknown error"))
                    }
                )
            }
        } catch (e: Exception) {
            flow { emit(GenerationState.Error(e.message ?: "Unknown error")) }
        }
    }
    
    suspend fun continueWriting(
        chapter: Chapter,
        title: String
    ): Flow<GenerationState> {
        return try {
            val systemPrompt = NovelPromptBuilder.buildSystemPrompt()
            val userPrompt = NovelPromptBuilder.buildContinuePrompt(
                previousContent = chapter.content,
                title = title
            )
            
            // 从设置中获取当前模型
            val currentModel = ApiClient.getModel()
            
            val request = GrokRequest(
                model = currentModel,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                stream = false,
                temperature = 0.8,
                maxTokens = 4096
            )
            
            flow {
                val result = streamingApiClient.generate(request)
                result.fold(
                    onSuccess = { continuedContent ->
                        val updatedChapter = chapter.copy(content = chapter.content + "\n\n" + continuedContent)
                        repository.updateChapter(updatedChapter)
                        emit(GenerationState.Success(updatedChapter))
                    },
                    onFailure = { e ->
                        emit(GenerationState.Error(e.message ?: "Unknown error"))
                    }
                )
            }
        } catch (e: Exception) {
            flow { emit(GenerationState.Error(e.message ?: "Unknown error")) }
        }
    }
}
