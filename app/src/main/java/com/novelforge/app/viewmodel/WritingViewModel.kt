package com.novelforge.app.viewmodel

import android.app.Application
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
import com.novelforge.app.util.NovelExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for Writing screen
 */
data class WritingUiState(
    val novel: Novel? = null,
    val chapters: List<Chapter> = emptyList(),
    val currentChapterIndex: Int = -1, // -1 means no chapter selected
    val displayContent: String = "",
    val isEditing: Boolean = false, // manual editing mode
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showChapterList: Boolean = false, // chapter list bottom sheet
    val showGuidanceSheet: Boolean = false, // AI generation bottom sheet
    val showNovelInfoSheet: Boolean = false, // novel info edit sheet
    val chapterGuidance: ChapterGuidance = ChapterGuidance(),
    val targetWordCount: Int = 2000,
    val editingChapterTitle: String = "", // for title editing dialog
    val showEditTitleDialog: Boolean = false,
    val showDeleteChapterDialog: Boolean = false,
    val chapterToDelete: Chapter? = null
) {
    /**
     * Get current chapter (if any)
     */
    val currentChapter: Chapter?
        get() = if (currentChapterIndex >= 0 && currentChapterIndex < chapters.size) {
            chapters[currentChapterIndex]
        } else null
    
    /**
     * Get word count of current content
     */
    val currentWordCount: Int
        get() = displayContent.length
    
    /**
     * Check if there's content to save
     */
    val hasUnsavedChanges: Boolean
        get() = displayContent.isNotBlank() && currentChapter != null && 
                displayContent != currentChapter?.content
}

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
                // Sort chapters by order
                val sortedChapters = chapters.sortedBy { it.order }
                
                // If we have a current chapter index and it might be out of bounds after reload
                val currentIndex = _uiState.value.currentChapterIndex
                val newIndex = when {
                    sortedChapters.isEmpty() -> -1
                    currentIndex < 0 -> sortedChapters.size - 1 // Select last chapter on first load
                    currentIndex >= sortedChapters.size -> sortedChapters.size - 1
                    else -> currentIndex
                }
                
                _uiState.value = _uiState.value.copy(
                    chapters = sortedChapters,
                    currentChapterIndex = newIndex,
                    displayContent = if (newIndex >= 0) sortedChapters[newIndex].content else ""
                )
            }
        }
    }
    
    // Chapter navigation
    fun selectChapter(index: Int) {
        if (index < 0 || index >= _uiState.value.chapters.size) return
        
        _uiState.value = _uiState.value.copy(
            currentChapterIndex = index,
            displayContent = _uiState.value.chapters[index].content,
            showChapterList = false
        )
    }
    
    // Content editing
    fun updateContent(text: String) {
        _uiState.value = _uiState.value.copy(
            displayContent = text,
            isEditing = true
        )
    }
    
    // Chapter list bottom sheet
    fun showChapterList() {
        _uiState.value = _uiState.value.copy(showChapterList = true)
    }
    
    fun dismissChapterList() {
        _uiState.value = _uiState.value.copy(showChapterList = false)
    }
    
    // Guidance bottom sheet
    fun showGuidanceSheet() {
        _uiState.value = _uiState.value.copy(
            showGuidanceSheet = true,
            chapterGuidance = ChapterGuidance()
        )
    }
    
    fun dismissGuidanceSheet() {
        _uiState.value = _uiState.value.copy(showGuidanceSheet = false)
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
    
    fun updateTargetWordCount(count: Int) {
        _uiState.value = _uiState.value.copy(targetWordCount = count)
    }
    
    // Novel info sheet
    fun showNovelInfoSheet() {
        _uiState.value = _uiState.value.copy(showNovelInfoSheet = true)
    }
    
    fun dismissNovelInfoSheet() {
        _uiState.value = _uiState.value.copy(showNovelInfoSheet = false)
    }
    
    fun updateNovelInfo(
        title: String,
        characterSetting: String,
        worldSetting: String
    ) {
        viewModelScope.launch {
            _uiState.value.novel?.let { novel ->
                val updated = novel.copy(
                    title = title,
                    characterSetting = characterSetting,
                    worldSetting = worldSetting
                )
                repository.updateNovel(updated)
                _uiState.value = _uiState.value.copy(
                    showNovelInfoSheet = false,
                    successMessage = "小说信息已更新"
                )
            }
        }
    }
    
    // Edit chapter title
    fun showEditTitleDialog(chapter: Chapter) {
        _uiState.value = _uiState.value.copy(
            showEditTitleDialog = true,
            editingChapterTitle = chapter.title
        )
    }
    
    fun dismissEditTitleDialog() {
        _uiState.value = _uiState.value.copy(
            showEditTitleDialog = false,
            editingChapterTitle = ""
        )
    }
    
    fun updateChapterTitle(newTitle: String) {
        viewModelScope.launch {
            val chapter = _uiState.value.currentChapter ?: return@launch
            val updated = chapter.copy(title = newTitle)
            repository.updateChapter(updated)
            dismissEditTitleDialog()
        }
    }
    
    // Delete chapter
    fun showDeleteChapterDialog(chapter: Chapter) {
        _uiState.value = _uiState.value.copy(
            showDeleteChapterDialog = true,
            chapterToDelete = chapter
        )
    }
    
    fun dismissDeleteChapterDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteChapterDialog = false,
            chapterToDelete = null
        )
    }
    
    fun deleteChapter() {
        viewModelScope.launch {
            val chapter = _uiState.value.chapterToDelete ?: return@launch
            repository.deleteChapter(chapter)
            dismissDeleteChapterDialog()
            
            // Refresh to update the list and select another chapter
            loadNovel(currentNovelId)
        }
    }
    
    // Generate new chapter
    fun generateNewChapter() {
        dismissGuidanceSheet()
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
            isEditing = false,
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
                chapterSummary = "",
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
                            displayContent = state.chapter.content,
                            successMessage = "第${state.chapter.order}章生成完成"
                        )
                        // Reload to update chapter list and select new chapter
                        loadNovel(currentNovelId)
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
    
    // Continue writing
    fun continueWriting() {
        val novel = _uiState.value.novel ?: return
        
        _uiState.value = _uiState.value.copy(
            isGenerating = true,
            isEditing = false,
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
                                displayContent = state.chapter.content,
                                successMessage = "续写完成"
                            )
                            // Reload to update
                            loadNovel(currentNovelId)
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
    
    // Save draft
    fun saveDraft() {
        viewModelScope.launch {
            val chapter = _uiState.value.currentChapter ?: return@launch
            val updated = chapter.copy(
                content = _uiState.value.displayContent,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateChapter(updated)
            _uiState.value = _uiState.value.copy(
                isEditing = false,
                successMessage = "保存成功"
            )
            // Reload to update
            loadNovel(currentNovelId)
        }
    }
    
    // Export novel
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
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
