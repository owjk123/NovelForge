package com.novelforge.app

import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.domain.prompt.NovelPromptBuilder
import com.novelforge.app.domain.prompt.NovelPromptParams
import org.junit.Test
import org.junit.Assert.*

class HomeViewModelTest {

    @Test
    fun `novel genre enum has all expected values`() {
        val genres = NovelGenre.values()
        assertEquals(5, genres.size)
        assertTrue(genres.any { it.name == "FANTASY" })
        assertTrue(genres.any { it.name == "SCIFI" })
        assertTrue(genres.any { it.name == "URBAN" })
        assertTrue(genres.any { it.name == "HAREM" })
        assertTrue(genres.any { it.name == "MYSTERY" })
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
    }

    @Test
    fun `system prompt is non-blank for all genres`() {
        NovelGenre.values().forEach { genre ->
            assertTrue(genre.systemPrompt.isNotBlank())
        }
    }
}
