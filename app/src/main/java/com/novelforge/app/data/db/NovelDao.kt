package com.novelforge.app.data.db

import androidx.room.*
import com.novelforge.app.data.model.Novel
import kotlinx.coroutines.flow.Flow

@Dao
interface NovelDao {
    
    @Query("SELECT * FROM novels ORDER BY updatedAt DESC")
    fun getAllNovels(): Flow<List<Novel>>
    
    @Query("SELECT * FROM novels WHERE id = :novelId")
    suspend fun getNovelById(novelId: Long): Novel?
    
    @Query("SELECT * FROM novels WHERE id = :novelId")
    fun getNovelByIdFlow(novelId: Long): Flow<Novel?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNovel(novel: Novel): Long
    
    @Update
    suspend fun updateNovel(novel: Novel)
    
    @Delete
    suspend fun deleteNovel(novel: Novel)
    
    @Query("DELETE FROM novels WHERE id = :novelId")
    suspend fun deleteNovelById(novelId: Long)
    
    @Query("UPDATE novels SET updatedAt = :timestamp WHERE id = :novelId")
    suspend fun updateNovelTimestamp(novelId: Long, timestamp: Long = System.currentTimeMillis())
}
