package com.novelforge.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelforge.app.data.db.AppDatabase
import com.novelforge.app.data.model.Novel
import com.novelforge.app.data.repository.NovelRepository
import com.novelforge.app.util.NovelExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val novels: List<Novel> = emptyList(),
    val isLoading: Boolean = true,
    val novelToDelete: Novel? = null,
    val novelToExport: Novel? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
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
    
    fun showExportConfirmation(novel: Novel) {
        _uiState.value = _uiState.value.copy(novelToExport = novel)
    }
    
    fun dismissExportConfirmation() {
        _uiState.value = _uiState.value.copy(novelToExport = null)
    }
    
    fun exportNovel(novel: Novel) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(novelToExport = null)
            
            try {
                val chapters = repository.getChaptersByNovelIdSync(novel.id)
                
                if (chapters.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "《${novel.title}》没有章节可以导出"
                    )
                    return@launch
                }
                
                val context = getApplication<Application>()
                val content = NovelExporter.buildExportContent(novel, chapters)
                val fileName = "${novel.title}.txt"
                
                val success = NovelExporter.saveToDownloads(context, fileName, content)
                
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "《${novel.title}》已导出到 Downloads/$fileName"
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
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
