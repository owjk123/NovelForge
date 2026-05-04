package com.novelforge.app

import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.domain.prompt.NovelPromptBuilder
import com.novelforge.app.domain.prompt.NovelPromptParams
import com.novelforge.app.domain.prompt.ChapterGuidance
import com.novelforge.app.domain.prompt.EmotionalTone
import org.junit.Test
import org.junit.Assert.*

class HomeViewModelTest {

    @Test
    fun `novel genre enum has all expected values`() {
        val genres = NovelGenre.entries
        assertEquals(6, genres.size)
        assertTrue(genres.any { it.name == "FANTASY" })
        assertTrue(genres.any { it.name == "SCIFI" })
        assertTrue(genres.any { it.name == "URBAN" })
        assertTrue(genres.any { it.name == "HAREM" })
        assertTrue(genres.any { it.name == "MYSTERY" })
        assertTrue(genres.any { it.name == "CUSTOM" })
    }

    @Test
    fun `prompt builder creates valid prompt for fantasy genre`() {
        val params = NovelPromptParams(
            genre = NovelGenre.FANTASY,
            title = "修仙之路",
            characterSetting = "少年天才修仙者",
            worldSetting = "九州大陆",
            isNewChapter = true,
            chapterTitle = "第一章"
        )
        val prompt = NovelPromptBuilder.buildUserPrompt(params)
        assertTrue(prompt.contains("修仙之路"))
        assertTrue(prompt.contains("少年天才修仙者"))
        assertTrue(prompt.contains("九州大陆"))
    }

    @Test
    fun `genre display names are in Chinese`() {
        assertEquals("玄幻", NovelGenre.FANTASY.displayName)
        assertEquals("科幻", NovelGenre.SCIFI.displayName)
        assertEquals("都市", NovelGenre.URBAN.displayName)
        assertEquals("后宫", NovelGenre.HAREM.displayName)
        assertEquals("悬疑", NovelGenre.MYSTERY.displayName)
        assertEquals("自定义", NovelGenre.CUSTOM.displayName)
    }

    @Test
    fun `system prompt is non-blank for all genres`() {
        NovelGenre.entries.forEach { genre ->
            assertTrue(genre.systemPrompt.isNotBlank())
        }
    }

    @Test
    fun `chapter guidance is included in prompt when provided`() {
        val params = NovelPromptParams(
            genre = NovelGenre.FANTASY,
            title = "修仙之路",
            characterSetting = "少年天才",
            worldSetting = "九州大陆",
            isNewChapter = true,
            chapterTitle = "第一章",
            chapterGuidance = ChapterGuidance(
                plotDirection = "主角突破境界",
                keyEvents = "遭遇强敌",
                emotionalTone = EmotionalTone.TENSE
            )
        )
        val prompt = NovelPromptBuilder.buildUserPrompt(params)
        assertTrue(prompt.contains("主角突破境界"))
        assertTrue(prompt.contains("遭遇强敌"))
        assertTrue(prompt.contains("紧张"))
    }

    @Test
    fun `auto fill prompt contains story description`() {
        val description = "一个少年在末世中觉醒异能"
        val prompt = NovelPromptBuilder.buildAutoFillPrompt(description)
        assertTrue(prompt.contains(description))
    }

    @Test
    fun `emotional tones have all expected values`() {
        val tones = EmotionalTone.entries
        assertEquals(6, tones.size)
        assertTrue(tones.any { it.name == "NEUTRAL" })
        assertTrue(tones.any { it.name == "TENSE" })
        assertTrue(tones.any { it.name == "WARM" })
        assertTrue(tones.any { it.name == "SAD" })
        assertTrue(tones.any { it.name == "COMEDIC" })
        assertTrue(tones.any { it.name == "PASSIONATE" })
    }
}
