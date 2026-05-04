package com.novelforge.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelforge.app.data.db.AppDatabase
import com.novelforge.app.data.model.Novel
import com.novelforge.app.data.repository.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val novels: List<Novel> = emptyList(),
    val isLoading: Boolean = true,
    val novelToDelete: Novel? = null,
    val errorMessage: String? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val repository = NovelRepository(
        database.novelDao(),
        database.chapterDao()
    )
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        loadNovels()
    }
    
    private fun loadNovels() {
        viewModelScope.launch {
            repository.getAllNovels().collect { novels ->
                _uiState.value = _uiState.value.copy(
                    novels = novels,
                    isLoading = false
                )
            }
        }
    }
    
    fun showDeleteConfirmation(novel: Novel) {
        _uiState.value = _uiState.value.copy(novelToDelete = novel)
    }
    
    fun dismissDeleteConfirmation() {
        _uiState.value = _uiState.value.copy(novelToDelete = null)
    }
    
    fun deleteNovel(novel: Novel) {
        viewModelScope.launch {
            try {
                repository.deleteNovel(novel)
                _uiState.value = _uiState.value.copy(novelToDelete = null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "删除失败",
                    novelToDelete = null
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
