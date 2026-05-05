package com.novelforge.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelforge.app.data.db.AppDatabase
import com.novelforge.app.data.model.Chapter
import com.novelforge.app.data.model.Novel
import com.novelforge.app.data.repository.NovelRepository
import com.novelforge.app.domain.prompt.ChapterGuidance
import com.novelforge.app.domain.prompt.EmotionalTone
import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.domain.usecase.GenerationState
import com.novelforge.app.domain.usecase.GenerateChapterUseCase
import com.novelforge.app.data.api.StreamingApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.novelforge.app.util.NovelExporter

data class WritingUiState(
    val novel: Novel? = null,
    val chapters: List<Chapter> = emptyList(),
    val currentChapter: Chapter? = null,
    val displayContent: String = "",
    val isGenerating: Boolean = false,
    val generationProgress: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val chapterSummary: String = "",
    val showGuidanceDialog: Boolean = false,
    val chapterGuidance: ChapterGuidance = ChapterGuidance()
)

class WritingViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val repository = NovelRepository(
        database.novelDao(),
        database.chapterDao()
    )
    private val streamingApiClient = StreamingApiClient()
    private val generateChapterUseCase = GenerateChapterUseCase(repository, streamingApiClient)
    
    private val _uiState = MutableStateFlow(WritingUiState())
    val uiState: StateFlow<WritingUiState> = _uiState.asStateFlow()
    
    private var currentNovelId: Long = 0
    
    fun loadNovel(novelId: Long) {
        currentNovelId = novelId
        viewModelScope.launch {
            repository.getNovelByIdFlow(novelId).collect { novel ->
                _uiState.value = _uiState.value.copy(novel = novel)
            }
        }
        viewModelScope.launch {
            repository.getChaptersByNovelId(novelId).collect { chapters ->
                _uiState.value = _uiState.value.copy(chapters = chapters)
            }
        }
    }
    
    fun showGuidanceDialog() {
        _uiState.value = _uiState.value.copy(
            showGuidanceDialog = true,
            chapterGuidance = ChapterGuidance()
        )
    }
    
    fun dismissGuidanceDialog() {
        _uiState.value = _uiState.value.copy(showGuidanceDialog = false)
    }
    
    fun updateGuidancePlotDirection(direction: String) {
        _uiState.value = _uiState.value.copy(
            chapterGuidance = _uiState.value.chapterGuidance.copy(plotDirection = direction)
        )
    }
    
    fun updateGuidanceKeyEvents(events: String) {
        _uiState.value = _uiState.value.copy(
            chapterGuidance = _uiState.value.chapterGuidance.copy(keyEvents = events)
        )
    }
    
    fun updateGuidanceEmotionalTone(tone: EmotionalTone) {
        _uiState.value = _uiState.value.copy(
            chapterGuidance = _uiState.value.chapterGuidance.copy(emotionalTone = tone)
        )
    }
    
    fun generateNewChapter() {
        dismissGuidanceDialog()
        val novel = _uiState.value.novel ?: return
        val guidance = _uiState.value.chapterGuidance
        
        val genre = try {
            if (novel.genre.startsWith("CUSTOM:")) {
                NovelGenre.CUSTOM
            } else {
                NovelGenre.valueOf(novel.genre)
            }
        } catch (e: Exception) {
            NovelGenre.FANTASY
        }
        
        _uiState.value = _uiState.value.copy(
            isGenerating = true,
            displayContent = "",
            errorMessage = null,
            successMessage = null
        )
        
        viewModelScope.launch {
            generateChapterUseCase.generateNewChapter(
                novelId = currentNovelId,
                genre = genre,
                title = novel.title,
                characterSetting = novel.characterSetting,
                worldSetting = novel.worldSetting,
                chapterSummary = _uiState.value.chapterSummary,
                chapterGuidance = guidance
            ).collect { state ->
                when (state) {
                    is GenerationState.Generating -> {
                        _uiState.value = _uiState.value.copy(
                            displayContent = state.partialContent
                        )
                    }
                    is GenerationState.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            currentChapter = state.chapter,
                            displayContent = state.chapter.content,
                            successMessage = "第${state.chapter.order}章生成完成",
                            chapterSummary = "",
                            chapterGuidance = ChapterGuidance()
                        )
                    }
                    is GenerationState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            errorMessage = state.message
                        )
                    }
                }
            }
        }
    }
    
    fun continueWriting() {
        val novel = _uiState.value.novel ?: return
        
        _uiState.value = _uiState.value.copy(
            isGenerating = true,
            errorMessage = null,
            successMessage = null
        )
        
        viewModelScope.launch {
            val lastChapter = _uiState.value.currentChapter 
                ?: repository.getLastChapter(currentNovelId)
                ?: run {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        errorMessage = "没有找到可以续写的章节"
                    )
                    return@launch
                }
            
            generateChapterUseCase.continueWriting(lastChapter, novel.title)
                .collect { state ->
                    when (state) {
                        is GenerationState.Generating -> {
                            _uiState.value = _uiState.value.copy(
                                displayContent = _uiState.value.currentChapter?.content.orEmpty() + 
                                    "\n\n" + state.partialContent
                            )
                        }
                        is GenerationState.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isGenerating = false,
                                currentChapter = state.chapter,
                                displayContent = state.chapter.content,
                                successMessage = "续写完成"
                            )
                        }
                        is GenerationState.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isGenerating = false,
                                errorMessage = state.message
                            )
                        }
                    }
                }
        }
    }
    
    fun updateChapterSummary(summary: String) {
        _uiState.value = _uiState.value.copy(chapterSummary = summary)
    }
    
    fun saveDraft() {
        viewModelScope.launch {
            _uiState.value.currentChapter?.let { chapter ->
                val updated = chapter.copy(
                    content = _uiState.value.displayContent,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateChapter(updated)
                _uiState.value = _uiState.value.copy(
                    currentChapter = updated,
                    successMessage = "保存成功"
                )
            }
        }
    }
    
    fun exportToDownloads() {
        viewModelScope.launch {
            val novel = _uiState.value.novel ?: return@launch
            val chapters = _uiState.value.chapters
            
            if (chapters.isEmpty()) {
                _uiState.value = _uiState.value.copy(errorMessage = "没有章节可以导出")
                return@launch
            }
            
            try {
                val context = getApplication<Application>()
                val content = NovelExporter.buildExportContent(novel, chapters)
                val fileName = "${novel.title}.txt"
                
                val success = NovelExporter.saveToDownloads(context, fileName, content)
                
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "已导出到 Downloads/$fileName"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "导出失败，请检查存储权限"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "导出失败: ${e.message}"
                )
            }
        }
    }
    
    private fun buildExportContent(novel: Novel, chapters: List<Chapter>): String {
        return buildString {
            appendLine("《${novel.title}》")
            appendLine("类型：${novel.genre}")
            appendLine()
            appendLine("【主角设定】")
            appendLine(novel.characterSetting)
            appendLine()
            appendLine("【世界观设定】")
            appendLine(novel.worldSetting)
            appendLine()
            appendLine("=".repeat(50))
            appendLine()
            
            chapters.sortedBy { it.order }.forEach { chapter ->
                appendLine("【${chapter.title}】")
                appendLine()
                appendLine(chapter.content)
                appendLine()
                appendLine("-".repeat(30))
                appendLine()
            }
            
            appendLine()
            appendLine("=" .repeat(50))
            appendLine("Generated by NovelForge")
        }
    }
    
    private fun saveToDownloads(context: Context, fileName: String, content: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore API
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                    true
                } ?: false
            } else {
                // Android 9 及以下使用传统方式
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
