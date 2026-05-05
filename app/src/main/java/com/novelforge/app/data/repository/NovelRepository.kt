package com.novelforge.app.data.repository

import com.novelforge.app.data.db.ChapterDao
import com.novelforge.app.data.db.NovelDao
import com.novelforge.app.data.model.Chapter
import com.novelforge.app.data.model.Novel
import kotlinx.coroutines.flow.Flow

class NovelRepository(
    private val novelDao: NovelDao,
    private val chapterDao: ChapterDao
) {
    // Novel operations
    fun getAllNovels(): Flow<List<Novel>> = novelDao.getAllNovels()
    
    suspend fun getNovelById(novelId: Long): Novel? = novelDao.getNovelById(novelId)
    
    fun getNovelByIdFlow(novelId: Long): Flow<Novel?> = novelDao.getNovelByIdFlow(novelId)
    
    suspend fun insertNovel(novel: Novel): Long = novelDao.insertNovel(novel)
    
    suspend fun updateNovel(novel: Novel) = novelDao.updateNovel(novel)
    
    suspend fun deleteNovel(novel: Novel) = novelDao.deleteNovel(novel)
    
    suspend fun deleteNovelById(novelId: Long) = novelDao.deleteNovelById(novelId)
    
    // Chapter operations
    fun getChaptersByNovelId(novelId: Long): Flow<List<Chapter>> = 
        chapterDao.getChaptersByNovelId(novelId)
    
    suspend fun getChaptersByNovelIdSync(novelId: Long): List<Chapter> = 
        chapterDao.getChaptersByNovelIdSync(novelId)
    
    suspend fun getChapterById(chapterId: Long): Chapter? = chapterDao.getChapterById(chapterId)
    
    suspend fun getMaxChapterOrder(novelId: Long): Int = 
        chapterDao.getMaxChapterOrder(novelId) ?: 0
    
    suspend fun insertChapter(chapter: Chapter): Long {
        val id = chapterDao.insertChapter(chapter)
        novelDao.updateNovelTimestamp(chapter.novelId)
        return id
    }
    
    suspend fun updateChapter(chapter: Chapter) {
        chapterDao.updateChapter(chapter)
        novelDao.updateNovelTimestamp(chapter.novelId)
    }
    
    suspend fun deleteChapter(chapter: Chapter) = chapterDao.deleteChapter(chapter)
    
    suspend fun getLastChapter(novelId: Long): Chapter? = chapterDao.getLastChapter(novelId)
    
    suspend fun getNextChapterOrder(novelId: Long): Int = getMaxChapterOrder(novelId) + 1
    
    // Statistics operations
    /**
     * Get statistics for a novel including chapter count and total word count
     */
    suspend fun getNovelStats(novelId: Long): NovelStats {
        val chapterCount = chapterDao.getChapterCount(novelId)
        val totalWordCount = chapterDao.getTotalWordCount(novelId) ?: 0
        return NovelStats(
            chapterCount = chapterCount,
            totalWordCount = totalWordCount
        )
    }
    
    /**
     * Get statistics for multiple novels
     */
    suspend fun getNovelStatsMap(novelIds: List<Long>): Map<Long, NovelStats> {
        return novelIds.associateWith { novelId ->
            getNovelStats(novelId)
        }
    }
}

/**
 * Statistics data class for a novel
 */
data class NovelStats(
    val chapterCount: Int,
    val totalWordCount: Int
) {
    /**
     * Format total word count to display string
     * e.g., 3200 -> "3.2万字"
     */
    fun getFormattedWordCount(): String {
        return when {
            totalWordCount >= 10000 -> String.format("%.1f万字", totalWordCount / 10000.0)
            totalWordCount >= 1000 -> String.format("%.1f千字", totalWordCount / 1000.0)
            else -> "${totalWordCount}字"
        }
    }
    
    /**
     * Format chapter count with "章" suffix
     */
    fun getFormattedChapterCount(): String {
        return "${chapterCount}章"
    }
}
