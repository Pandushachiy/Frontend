package com.health.companion.data.local.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long? = null,  // Время последнего сообщения
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val summary: String? = null
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val content: String,
    val role: String, // "user", "assistant"
    val agentName: String? = null,
    val confidence: Float? = null,
    val sources: String? = null, // JSON array stored as string
    val provider: String? = null,
    val providerColor: String? = null,
    val modelUsed: String? = null,
    val tokensUsed: Int? = null,
    val processingTime: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,  // URL сгенерированного AI изображения
    val images: String? = null  // JSON array URLs прикреплённых пользователем изображений
)

@Entity(tableName = "health_metrics")
data class HealthMetricEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val metricType: String,
    val value: Float,
    val unit: String,
    val source: String = "manual", // "manual", "wearable", "ai_prediction"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val moodLevel: Int, // 1-5
    val symptoms: String, // JSON array stored as string
    val journalText: String,
    val stressLevel: Int, // 1-5
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val filename: String,
    val documentType: String,
    val status: String, // "uploading", "processing", "processed", "error"
    val extractedText: String? = null,
    val filePath: String,
    val uploadedAt: Long = System.currentTimeMillis()
)
