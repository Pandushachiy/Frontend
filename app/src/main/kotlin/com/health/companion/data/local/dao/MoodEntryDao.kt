package com.health.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.health.companion.data.local.database.MoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodEntryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MoodEntryEntity)
    
    @Query("SELECT * FROM mood_entries ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentEntriesFlow(limit: Int = 30): Flow<List<MoodEntryEntity>>
    
    @Query("SELECT * FROM mood_entries ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentEntries(limit: Int = 30): List<MoodEntryEntity>
    
    @Query("SELECT * FROM mood_entries WHERE createdAt BETWEEN :startTime AND :endTime ORDER BY createdAt DESC")
    suspend fun getEntriesInRange(startTime: Long, endTime: Long): List<MoodEntryEntity>
    
    @Query("SELECT AVG(moodLevel) FROM mood_entries WHERE createdAt > :sinceTime")
    suspend fun getAverageMoodSince(sinceTime: Long): Float?
    
    @Query("DELETE FROM mood_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: String)
}
