package com.novelforge.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelforge.app.data.db.AppDatabase
import com.novelforge.app.data.model.Chapter
import com.novelforge.app.data.model.Novel
import com.novelforge.app.data.repository.NovelRepository
import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.domain.usecase.GenerationState
import com.novelforge.app.domain.usecase.GenerateChapterUseCase
import com.novelforge.app.data.api.StreamingApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WritingUiState(
    val novel: Novel? = null,
    val chapters: List<Chapter> = emptyList(),
    val currentChapter: Chapter? = null,
    val displayContent: String = "",
    val isGenerating: Boolean = false,
    val generationProgress: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val chapterSummary: String = ""
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
    
    fun generateNewChapter() {
        val novel = _uiState.value.novel ?: return
        val genre = try {
            NovelGenre.valueOf(novel.genre)
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
                chapterSummary = _uiState.value.chapterSummary
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
                            chapterSummary = ""
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
        val lastChapter = _uiState.value.currentChapter
            ?: viewModelScope.launch {
                repository.getLastChapter(currentNovelId)
            }.let { return }
        
        val novel = _uiState.value.novel ?: return
        
        _uiState.value = _uiState.value.copy(
            isGenerating = true,
            displayContent = lastChapter.content,
            errorMessage = null,
            successMessage = null
        )
        
        viewModelScope.launch {
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
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
