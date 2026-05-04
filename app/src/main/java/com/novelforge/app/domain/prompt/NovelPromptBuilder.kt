package com.novelforge.app.domain.prompt

import com.novelforge.app.data.model.Chapter

enum class NovelGenre(val displayName: String, val systemPrompt: String) {
    FANTASY("玄幻", "仙侠修真"),
    SCIFI("科幻", "未来科技"),
    URBAN("都市", "现代都市"),
    HAREM("后宫", "都市情感"),
    MYSTERY("悬疑", "推理探案"),
    CUSTOM("自定义", "用户自定义类型")
}

data class NovelPromptParams(
    val genre: NovelGenre,
    val title: String,
    val characterSetting: String,
    val worldSetting: String,
    val chapterSummary: String = "",
    val previousContent: String = "",
    val targetWordCount: Int = 2000,
    val isNewChapter: Boolean = true,
    val chapterTitle: String = "",
    val chapterGuidance: ChapterGuidance? = null
)

// 章节引导数据类
data class ChapterGuidance(
    val plotDirection: String = "",       // 本章剧情方向（必填）
    val keyEvents: String = "",           // 关键事件/转折（可选）
    val emotionalTone: EmotionalTone = EmotionalTone.NEUTRAL  // 情感基调
)

enum class EmotionalTone(val displayName: String) {
    NEUTRAL("中性"),
    TENSE("紧张"),
    WARM("温馨"),
    SAD("悲伤"),
    COMEDIC("搞笑"),
    PASSIONATE("热血")
}

object NovelPromptBuilder {
    
    fun buildSystemPrompt(): String {
        return """你是一位经验丰富的网络小说作家，精通各种类型小说的创作。
你擅长构建宏大的世界观、塑造鲜明的人物性格、编织扣人心弦的剧情。
请用生动的语言、细腻的描写来创作小说内容。
每章内容应该字数在2000-3000字之间，情节丰富、描写细腻。
请直接输出小说内容，不要输出任何额外说明。"""
    }
    
    fun buildUserPrompt(params: NovelPromptParams): String {
        val genreDescription = params.genre.systemPrompt
        
        return buildString {
            appendLine("【小说信息】")
            appendLine("标题：${params.title}")
            appendLine("类型：$genreDescription")
            appendLine()
            
            appendLine("【主角设定】")
            appendLine(params.characterSetting)
            appendLine()
            
            appendLine("【世界观设定】")
            appendLine(params.worldSetting)
            appendLine()
            
            if (params.isNewChapter) {
                if (params.chapterSummary.isNotBlank()) {
                    appendLine("【本章剧情摘要】")
                    appendLine(params.chapterSummary)
                    appendLine()
                }
                
                // 添加章节引导信息
                params.chapterGuidance?.let { guidance ->
                    if (guidance.plotDirection.isNotBlank()) {
                        appendLine("【本章剧情方向】")
                        appendLine(guidance.plotDirection)
                        appendLine()
                    }
                    if (guidance.keyEvents.isNotBlank()) {
                        appendLine("【关键事件/转折】")
                        appendLine(guidance.keyEvents)
                        appendLine()
                    }
                    if (guidance.emotionalTone != EmotionalTone.NEUTRAL) {
                        appendLine("【情感基调】")
                        appendLine(guidance.emotionalTone.displayName)
                        appendLine()
                    }
                }
                
                if (params.previousContent.isNotBlank()) {
                    appendLine("【前情提要】")
                    appendLine(params.previousContent.takeLast(500))
                    appendLine()
                }
                
                val chapterNum = params.chapterTitle.ifBlank { "新章节" }
                appendLine("【写作要求】")
                appendLine("请创作「第${chapterNum}」章的内容。")
                appendLine("本章字数目标：${params.targetWordCount}字左右。")
                appendLine("要求情节连贯、高潮迭起、描写细腻。")
            } else {
                appendLine("【续写要求】")
                appendLine("请续写以下内容，保持文风一致、情节连贯自然。")
                appendLine()
                appendLine("【已有内容】")
                appendLine(params.previousContent)
            }
        }
    }
    
    fun buildContinuePrompt(previousContent: String, title: String): String {
        return buildString {
            appendLine("请续写以下小说内容，保持文风一致、情节连贯自然。")
            appendLine()
            appendLine("【小说标题】$title")
            appendLine()
            appendLine("【已有内容】")
            appendLine(previousContent)
        }
    }
    
    // 构建AI自动填充的prompt
    fun buildAutoFillPrompt(storyDescription: String): String {
        return buildString {
            appendLine("请根据以下小说故事描述，自动生成适合的小说设定。")
            appendLine()
            appendLine("【故事描述】")
            appendLine(storyDescription)
            appendLine()
            appendLine("请按以下JSON格式返回（只返回JSON，不要其他内容）：")
            appendLine("""
{
    "genre": "推荐的小说类型（玄幻/科幻/都市/后宫/悬疑）",
    "characterSetting": "主角设定，包括姓名、性格特点、背景故事等，100-200字",
    "worldSetting": "世界观设定，包括故事发生的背景、规则、特色设定等，100-200字"
}
            """.trimIndent())
        }
    }
    
    // 解析AI自动填充的响应
    fun parseAutoFillResponse(response: String): AutoFillResult? {
        return try {
            // 尝试提取JSON部分
            val jsonStr = response
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            val genreMatch = Regex("\"genre\"\\s*:\\s*\"([^\"]+)\"").find(jsonStr)
            val characterMatch = Regex("\"characterSetting\"\\s*:\\s*\"([^\"]+)\"").find(jsonStr)
            val worldMatch = Regex("\"worldSetting\"\\s*:\\s*\"([^\"]+)\"").find(jsonStr)
            
            if (genreMatch != null && characterMatch != null && worldMatch != null) {
                val genreStr = genreMatch.groupValues[1]
                val genre = try {
                    NovelGenre.valueOf(genreStr.uppercase())
                } catch (e: Exception) {
                    NovelGenre.CUSTOM
                }
                
                AutoFillResult(
                    genre = genre,
                    genreDisplayName = genreStr,
                    characterSetting = characterMatch.groupValues[1],
                    worldSetting = worldMatch.groupValues[1]
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

data class AutoFillResult(
    val genre: NovelGenre,
    val genreDisplayName: String,
    val characterSetting: String,
    val worldSetting: String
)
