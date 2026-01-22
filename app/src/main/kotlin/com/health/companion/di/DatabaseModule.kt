package com.health.companion.di

import android.content.Context
import androidx.room.Room
import com.health.companion.data.local.database.HealthCompanionDatabase
import com.health.companion.data.local.dao.ChatMessageDao
import com.health.companion.data.local.dao.ConversationDao
import com.health.companion.data.local.dao.DocumentDao
import com.health.companion.data.local.dao.HealthMetricDao
import com.health.companion.data.local.dao.MoodEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ): HealthCompanionDatabase = Room.databaseBuilder(
        context,
        HealthCompanionDatabase::class.java,
        "health_companion.db"
    )
        .fallbackToDestructiveMigration()
        .build()
    
    @Singleton
    @Provides
    fun provideChatMessageDao(database: HealthCompanionDatabase): ChatMessageDao =
        database.chatMessageDao()
    
    @Singleton
    @Provides
    fun provideConversationDao(database: HealthCompanionDatabase): ConversationDao =
        database.conversationDao()
    
    @Singleton
    @Provides
    fun provideHealthMetricDao(database: HealthCompanionDatabase): HealthMetricDao =
        database.healthMetricDao()
    
    @Singleton
    @Provides
    fun provideMoodEntryDao(database: HealthCompanionDatabase): MoodEntryDao =
        database.moodEntryDao()
    
    @Singleton
    @Provides
    fun provideDocumentDao(database: HealthCompanionDatabase): DocumentDao =
        database.documentDao()
}
