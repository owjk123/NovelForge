package com.novelforge.app.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novelforge.app.data.repository.NovelStats
import com.novelforge.app.domain.prompt.NovelGenre
import com.novelforge.app.data.repository.NovelRepository
import com.novelforge.app.data.db.AppDatabase
import com.novelforge.app.data.model.Chapter
import com.novelforge.app.data.model.Novel
import com.novelforge.app.util.NovelExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Sorting options for library
 */
enum class SortType {
    LAST_UPDATED,  // 最近更新
    CREATED_TIME, // 创建时间
    TITLE          // 标题
}

/**
 * UI State for Library screen
 */
data class LibraryUiState(
    val novels: List<Novel> = emptyList(),
    val novelStats: Map<Long, NovelStats> = emptyMap(), // novelId -> stats
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedGenre: NovelGenre? = null, // null = 全部
    val sortType: SortType = SortType.LAST_UPDATED,
    val showContextMenu: Boolean = false,
    val selectedNovelForMenu: Novel? = null,
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
    
    // Store all novels from database
    private var allNovels: List<Novel> = emptyList()
    
    init {
        loadNovels()
    }
    
    private fun loadNovels() {
        viewModelScope.launch {
            repository.getAllNovels().collect { novels ->
                allNovels = novels
                // Load stats for all novels
                val statsMap = if (novels.isNotEmpty()) {
                    repository.getNovelStatsMap(novels.map { it.id })
                } else {
                    emptyMap()
                }
                updateFilteredNovels(statsMap)
            }
        }
    }
    
    private fun updateFilteredNovels(statsMap: Map<Long, NovelStats>? = null) {
        val currentState = _uiState.value
        val stats = statsMap ?: currentState.novelStats
        
        // Filter and sort novels
        val filteredNovels = allNovels
            .filter { novel ->
                // Filter by search query (title contains)
                val matchesSearch = currentState.searchQuery.isBlank() ||
                        novel.title.contains(currentState.searchQuery, ignoreCase = true)
                
                // Filter by genre
                val matchesGenre = currentState.selectedGenre == null ||
                        isNovelOfGenre(novel.genre, currentState.selectedGenre)
                
                matchesSearch && matchesGenre
            }
            .let { filtered ->
                // Sort novels
                when (currentState.sortType) {
                    SortType.LAST_UPDATED -> filtered.sortedByDescending { it.updatedAt }
                    SortType.CREATED_TIME -> filtered.sortedByDescending { it.createdAt }
                    SortType.TITLE -> filtered.sortedBy { it.title.lowercase() }
                }
            }
        
        _uiState.value = currentState.copy(
            novels = filteredNovels,
            novelStats = stats,
            isLoading = false
        )
    }
    
    private fun isNovelOfGenre(novelGenre: String, selectedGenre: NovelGenre): Boolean {
        return when (selectedGenre) {
            NovelGenre.CUSTOM -> novelGenre.startsWith("CUSTOM:")
            else -> novelGenre.equals(selectedGenre.name, ignoreCase = true)
        }
    }
    
    // Search query update
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        viewModelScope.launch {
            updateFilteredNovels()
        }
    }
    
    // Genre filter update
    fun updateGenreFilter(genre: NovelGenre?) {
        _uiState.value = _uiState.value.copy(selectedGenre = genre)
        viewModelScope.launch {
            updateFilteredNovels()
        }
    }
    
    // Sort type update
    fun updateSortType(sortType: SortType) {
        _uiState.value = _uiState.value.copy(sortType = sortType)
        viewModelScope.launch {
            updateFilteredNovels()
        }
    }
    
    // Context menu operations
    fun showContextMenu(novel: Novel) {
        _uiState.value = _uiState.value.copy(
            showContextMenu = true,
            selectedNovelForMenu = novel
        )
    }
    
    fun dismissContextMenu() {
        _uiState.value = _uiState.value.copy(
            showContextMenu = false,
            selectedNovelForMenu = null
        )
    }
    
    // Delete operations
    fun showDeleteConfirmation(novel: Novel) {
        dismissContextMenu()
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
    
    // Export operations
    fun showExportConfirmation(novel: Novel) {
        dismissContextMenu()
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
                val content = buildExportContent(novel, chapters)
                val fileName = "${novel.title}.txt"
                
                val success = saveToDownloads(context, fileName, content)
                
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
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}
