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
}
