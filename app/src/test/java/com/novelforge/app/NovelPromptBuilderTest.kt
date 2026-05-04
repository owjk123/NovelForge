package com.novelforge.app

import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.domain.prompt.NovelPromptBuilder
import com.novelforge.app.domain.prompt.NovelPromptParams
import com.novelforge.app.domain.prompt.ChapterGuidance
import com.novelforge.app.domain.prompt.EmotionalTone
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
        assertTrue(userPrompt.contains(NovelGenre.FANTASY.systemPrompt))
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
    fun `all genres have valid display names and system prompts`() {
        NovelGenre.entries.forEach { genre ->
            assertTrue(
                "Genre ${genre.name} should have non-blank displayName",
                genre.displayName.isNotBlank()
            )
            assertTrue(
                "Genre ${genre.name} should have non-blank systemPrompt",
                genre.systemPrompt.isNotBlank()
            )
        }
    }

    @Test
    fun `chapter guidance is included in user prompt`() {
        val params = NovelPromptParams(
            genre = NovelGenre.FANTASY,
            title = "Test Novel",
            characterSetting = "Hero",
            worldSetting = "Fantasy world",
            isNewChapter = true,
            chapterTitle = "Chapter 1",
            chapterGuidance = ChapterGuidance(
                plotDirection = "Hero defeats the villain",
                keyEvents = "Battle scene",
                emotionalTone = EmotionalTone.PASSIONATE
            )
        )

        val userPrompt = NovelPromptBuilder.buildUserPrompt(params)

        assertTrue(userPrompt.contains("Hero defeats the villain"))
        assertTrue(userPrompt.contains("Battle scene"))
        assertTrue(userPrompt.contains("热血"))
    }

    @Test
    fun `auto fill prompt format is correct`() {
        val description = "A story about dragons"
        val prompt = NovelPromptBuilder.buildAutoFillPrompt(description)

        assertTrue(prompt.contains("小说故事描述"))
        assertTrue(prompt.contains(description))
        assertTrue(prompt.contains("genre"))
        assertTrue(prompt.contains("characterSetting"))
        assertTrue(prompt.contains("worldSetting"))
    }
}
