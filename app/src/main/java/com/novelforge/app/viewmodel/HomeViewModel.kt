package com.novelforge.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelforge.app.data.db.AppDatabase
import com.novelforge.app.data.model.Novel
import com.novelforge.app.data.repository.NovelRepository
import com.novelforge.app.domain.prompt.NovelGenre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val title: String = "",
    val selectedGenre: NovelGenre = NovelGenre.FANTASY,
    val characterSetting: String = "",
    val worldSetting: String = "",
    val isCreating: Boolean = false,
    val createdNovelId: Long? = null,
    val errorMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val repository = NovelRepository(
        database.novelDao(),
        database.chapterDao()
    )
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }
    
    fun updateGenre(genre: NovelGenre) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
    }
    
    fun updateCharacterSetting(setting: String) {
        _uiState.value = _uiState.value.copy(characterSetting = setting)
    }
    
    fun updateWorldSetting(setting: String) {
        _uiState.value = _uiState.value.copy(worldSetting = setting)
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
                val novel = Novel(
                    title = state.title,
                    genre = state.selectedGenre.name,
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
    
    fun resetCreatedNovelId() {
        _uiState.value = _uiState.value.copy(createdNovelId = null)
    }
}
