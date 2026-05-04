package com.novelforge.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelforge.app.data.api.ApiClient
import com.novelforge.app.data.api.GrokRequest
import com.novelforge.app.data.api.Message
import com.novelforge.app.data.api.StreamingApiClient
import com.novelforge.app.data.db.AppDatabase
import com.novelforge.app.data.model.Novel
import com.novelforge.app.data.repository.NovelRepository
import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.domain.prompt.NovelPromptBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val title: String = "",
    val selectedGenre: NovelGenre = NovelGenre.FANTASY,
    val customGenreName: String = "",
    val characterSetting: String = "",
    val worldSetting: String = "",
    val storyDescription: String = "",
    val isCreating: Boolean = false,
    val isAutoFilling: Boolean = false,
    val createdNovelId: Long? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val repository = NovelRepository(
        database.novelDao(),
        database.chapterDao()
    )
    private val streamingApiClient = StreamingApiClient()
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }
    
    fun updateGenre(genre: NovelGenre) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
    }
    
    fun updateCustomGenreName(name: String) {
        _uiState.value = _uiState.value.copy(customGenreName = name)
    }
    
    fun updateCharacterSetting(setting: String) {
        _uiState.value = _uiState.value.copy(characterSetting = setting)
    }
    
    fun updateWorldSetting(setting: String) {
        _uiState.value = _uiState.value.copy(worldSetting = setting)
    }
    
    fun updateStoryDescription(description: String) {
        _uiState.value = _uiState.value.copy(storyDescription = description)
    }
    
    fun autoFillWithAI() {
        val state = _uiState.value
        if (state.storyDescription.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请先输入故事描述")
            return
        }
        
        _uiState.value = state.copy(isAutoFilling = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                val systemPrompt = "你是一个专业的小说策划助手，擅长根据故事描述生成合适的小说设定。"
                val userPrompt = NovelPromptBuilder.buildAutoFillPrompt(state.storyDescription)
                
                val request = GrokRequest(
                    model = ApiClient.getModel(),
                    messages = listOf(
                        Message(role = "system", content = systemPrompt),
                        Message(role = "user", content = userPrompt)
                    ),
                    stream = false,
                    temperature = 0.7,
                    maxTokens = 2048
                )
                
                val result = streamingApiClient.generate(request)
                result.fold(
                    onSuccess = { response ->
                        val fillResult = NovelPromptBuilder.parseAutoFillResponse(response)
                        if (fillResult != null) {
                            _uiState.value = _uiState.value.copy(
                                isAutoFilling = false,
                                selectedGenre = fillResult.genre,
                                customGenreName = if (fillResult.genre == NovelGenre.CUSTOM) fillResult.genreDisplayName else "",
                                characterSetting = fillResult.characterSetting,
                                worldSetting = fillResult.worldSetting,
                                successMessage = "AI自动填充成功！"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isAutoFilling = false,
                                errorMessage = "解析AI响应失败，请重试"
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isAutoFilling = false,
                            errorMessage = e.message ?: "AI填充失败"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAutoFilling = false,
                    errorMessage = e.message ?: "AI填充失败"
                )
            }
        }
    }
    
    fun createNovel(onSuccess: (Long) -> Unit) {
        val state = _uiState.value
        
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入小说标题")
            return
        }
        
        if (state.characterSetting.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入主角设定")
            return
        }
        
        if (state.worldSetting.isBlank()) {
            _uiState.value = state.copy(errorMessage = "请输入世界观设定")
            return
        }
        
        _uiState.value = state.copy(isCreating = true, errorMessage = null)
        
        viewModelScope.launch {
            try {
                // 确定最终的类型
                val finalGenre = if (state.selectedGenre == NovelGenre.CUSTOM) {
                    "CUSTOM:${state.customGenreName}"
                } else {
                    state.selectedGenre.name
                }
                
                val novel = Novel(
                    title = state.title,
                    genre = finalGenre,
                    characterSetting = state.characterSetting,
                    worldSetting = state.worldSetting
                )
                val novelId = repository.insertNovel(novel)
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    createdNovelId = novelId
                )
                onSuccess(novelId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCreating = false,
                    errorMessage = e.message ?: "创建失败"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
    
    fun resetCreatedNovelId() {
        _uiState.value = _uiState.value.copy(createdNovelId = null)
    }
}
