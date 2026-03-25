package com.health.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.health.companion.data.local.database.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)
    
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesFlow(conversationId: String): Flow<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessages(conversationId: String): List<ChatMessageEntity>
    
    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: String)
    
    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId AND id NOT IN (:keepIds)")
    suspend fun deleteOrphans(conversationId: String, keepIds: List<String>)
    
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()

    // ─── Streaming draft support ───────────────────────────────────────────
    // Allows saving partial SSE content to Room every ~2s so that if the
    // process is killed mid-stream (OEM aggressive memory management), the
    // partial response survives and is shown to the user on next app start.

    @Query("UPDATE chat_messages SET content = :content, imageUrl = :imageUrl, filesJson = :filesJson WHERE id = :id")
    suspend fun updateStreamingContent(id: String, content: String, imageUrl: String?, filesJson: String?)

    @Query("UPDATE chat_messages SET isStreamingDraft = 0 WHERE conversationId = :conversationId")
    suspend fun clearStreamingDrafts(conversationId: String)

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId AND isStreamingDraft = 1 LIMIT 1")
    suspend fun getStreamingDraft(conversationId: String): ChatMessageEntity?
}
