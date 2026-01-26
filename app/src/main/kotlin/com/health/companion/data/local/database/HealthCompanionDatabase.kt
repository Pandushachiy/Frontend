package com.health.companion.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.health.companion.data.local.dao.ChatMessageDao
import com.health.companion.data.local.dao.ConversationDao
import com.health.companion.data.local.dao.DocumentDao
import com.health.companion.data.local.dao.HealthMetricDao
import com.health.companion.data.local.dao.MoodEntryDao

@Database(
    entities = [
        ConversationEntity::class,
        ChatMessageEntity::class,
        HealthMetricEntity::class,
        MoodEntryEntity::class,
        DocumentEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HealthCompanionDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun moodEntryDao(): MoodEntryDao
    abstract fun documentDao(): DocumentDao
}
