package com.novelforge.app.data.db

import androidx.room.*
import com.novelforge.app.data.model.Chapter
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY `order` ASC")
    fun getChaptersByNovelId(novelId: Long): Flow<List<Chapter>>
    
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY `order` ASC")
    suspend fun getChaptersByNovelIdSync(novelId: Long): List<Chapter>
    
    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: Long): Chapter?
    
    @Query("SELECT MAX(`order`) FROM chapters WHERE novelId = :novelId")
    suspend fun getMaxChapterOrder(novelId: Long): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: Chapter): Long
    
    @Update
    suspend fun updateChapter(chapter: Chapter)
    
    @Delete
    suspend fun deleteChapter(chapter: Chapter)
    
    @Query("DELETE FROM chapters WHERE novelId = :novelId")
    suspend fun deleteChaptersByNovelId(novelId: Long)
    
    @Query("SELECT * FROM chapters WHERE novelId = :novelId ORDER BY `order` DESC LIMIT 1")
    suspend fun getLastChapter(novelId: Long): Chapter?
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM chapters WHERE novelId = :novelId")
    suspend fun getChapterCount(novelId: Long): Int
    
    @Query("SELECT SUM(LENGTH(content)) FROM chapters WHERE novelId = :novelId")
    suspend fun getTotalWordCount(novelId: Long): Int?
}
