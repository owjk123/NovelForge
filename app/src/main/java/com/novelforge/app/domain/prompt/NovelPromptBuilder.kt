package com.novelforge.app.domain.prompt

enum class NovelGenre(val displayName: String, val systemPrompt: String) {
    FANTASY("玄幻", "仙侠修真"),
    SCIFI("科幻", "未来科技"),
    URBAN("都市", "现代都市"),
    HAREM("后宫", "都市情感"),
    MYSTERY("悬疑", "推理探案")
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
    val chapterTitle: String = ""
)

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
}
