package com.health.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.health.companion.data.local.database.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(conversation: ConversationEntity): Long

    @Update
    suspend fun update(conversation: ConversationEntity)

    @Transaction
    suspend fun upsert(conversation: ConversationEntity) {
        val rowId = insertIgnore(conversation)
        if (rowId == -1L) {
            update(conversation)
        }
    }
    
    @Query("SELECT * FROM conversations ORDER BY COALESCE(lastMessageAt, updatedAt) DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations ORDER BY COALESCE(lastMessageAt, updatedAt) DESC")
    suspend fun getAllConversations(): List<ConversationEntity>
    
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): ConversationEntity?
    
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteById(conversationId: String)

    @Query("UPDATE conversations SET updatedAt = :updatedAt, lastMessageAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateUpdatedAt(conversationId: String, updatedAt: Long)
    
    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :conversationId")
    suspend fun updateTitle(conversationId: String, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE conversations SET summary = :summary WHERE id = :conversationId")
    suspend fun updateSummary(conversationId: String, summary: String)
    
    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}
