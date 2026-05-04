package com.novelforge.app

import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.domain.prompt.NovelPromptBuilder
import com.novelforge.app.domain.prompt.NovelPromptParams
import org.junit.Test
import org.junit.Assert.*

class NovelPromptBuilderTest {
    
    @Test
    fun `buildSystemPrompt returns non-empty string`() {
        val systemPrompt = NovelPromptBuilder.buildSystemPrompt()
        assertTrue(systemPrompt.isNotBlank())
        assertTrue(systemPrompt.length > 50)
    }
    
    @Test
    fun `buildUserPrompt for new chapter includes title and genre`() {
        val params = NovelPromptParams(
            genre = NovelGenre.FANTASY,
            title = "Test Novel",
            characterSetting = "Hero: A brave warrior",
            worldSetting = "Medieval fantasy world",
            isNewChapter = true,
            chapterTitle = "Chapter 1"
        )
        
        val userPrompt = NovelPromptBuilder.buildUserPrompt(params)
        
        assertTrue(userPrompt.contains("Test Novel"))
        assertTrue(userPrompt.contains("玄幻"))
        assertTrue(userPrompt.contains("Hero: A brave warrior"))
        assertTrue(userPrompt.contains("Medieval fantasy world"))
        assertTrue(userPrompt.contains("Chapter 1"))
    }
    
    @Test
    fun `buildUserPrompt includes chapter summary when provided`() {
        val params = NovelPromptParams(
            genre = NovelGenre.SCIFI,
            title = "Space Adventure",
            characterSetting = "Captain: A fearless leader",
            worldSetting = "Future galaxy",
            chapterSummary = "The crew discovers a new planet",
            isNewChapter = true
        )
        
        val userPrompt = NovelPromptBuilder.buildUserPrompt(params)
        
        assertTrue(userPrompt.contains("本章剧情摘要"))
        assertTrue(userPrompt.contains("The crew discovers a new planet"))
    }
    
    @Test
    fun `buildContinuePrompt includes previous content`() {
        val previousContent = "Once upon a time in a distant land..."
        val title = "My Novel"
        
        val continuePrompt = NovelPromptBuilder.buildContinuePrompt(previousContent, title)
        
        assertTrue(continuePrompt.contains("续写"))
        assertTrue(continuePrompt.contains("My Novel"))
        assertTrue(continuePrompt.contains(previousContent))
    }
    
    @Test
    fun `all genres are supported`() {
        NovelGenre.entries.forEach { genre ->
            val params = NovelPromptParams(
                genre = genre,
                title = "Test",
                characterSetting = "Character",
                worldSetting = "World",
                isNewChapter = true
            )
            
            val userPrompt = NovelPromptBuilder.buildUserPrompt(params)
            assertTrue(
                "Genre ${genre.name} should be in prompt",
                userPrompt.contains(genre.displayName)
            )
        }
    }
}
