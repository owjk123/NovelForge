package com.novelforge.app.domain.usecase

import com.novelforge.app.data.api.GrokRequest
import com.novelforge.app.data.api.Message
import com.novelforge.app.data.api.StreamingApiClient
import com.novelforge.app.data.api.StreamResult
import com.novelforge.app.data.model.Chapter
import com.novelforge.app.data.repository.NovelRepository
import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.domain.prompt.NovelPromptBuilder
import com.novelforge.app.domain.prompt.NovelPromptParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
        targetWordCount: Int = 2000
    ): Flow<GenerationState> {
        return try {
            val previousChapters = repository.getChaptersByNovelIdSync(novelId)
            val previousContent = previousChapters.lastOrNull()?.content ?: ""
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
                    chapterTitle = "${chapterOrder}章"
                )
            )
            
            val request = GrokRequest(
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                stream = true,
                temperature = 0.8,
                maxTokens = 4096
            )
            
            var fullContent = ""
            
            repository.streamGenerate(request).map { result ->
                when (result) {
                    is StreamResult.OnNext -> {
                        fullContent += result.content
                        GenerationState.Generating(result.content)
                    }
                    is StreamResult.OnError -> {
                        GenerationState.Error(result.error)
                    }
                    is StreamResult.OnComplete -> {
                        val chapter = Chapter(
                            novelId = novelId,
                            order = chapterOrder,
                            title = chapterTitle.ifBlank { "第${chapterOrder}章" },
                            content = fullContent,
                            summary = chapterSummary
                        )
                        repository.insertChapter(chapter)
                        GenerationState.Success(chapter)
                    }
                }
            }
        } catch (e: Exception) {
            flowOf(GenerationState.Error(e.message ?: "Unknown error"))
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
            
            val request = GrokRequest(
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                stream = true,
                temperature = 0.8,
                maxTokens = 4096
            )
            
            var fullContent = ""
            
            repository.streamGenerate(request).map { result ->
                when (result) {
                    is StreamResult.OnNext -> {
                        fullContent += result.content
                        GenerationState.Generating(result.content)
                    }
                    is StreamResult.OnError -> {
                        GenerationState.Error(result.error)
                    }
                    is StreamResult.OnComplete -> {
                        val updatedChapter = chapter.copy(
                            content = chapter.content + "\n\n" + fullContent,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.updateChapter(updatedChapter)
                        GenerationState.Success(updatedChapter)
                    }
                }
            }
        } catch (e: Exception) {
            flowOf(GenerationState.Error(e.message ?: "Unknown error"))
        }
    }
    
    private fun <T> flowOf(value: T) = kotlinx.coroutines.flow.flowOf(value)
}

sealed class GenerationState {
    data class Generating(val partialContent: String) : GenerationState()
    data class Success(val chapter: Chapter) : GenerationState()
    data class Error(val message: String) : GenerationState()
}
