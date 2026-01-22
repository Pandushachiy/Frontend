package com.health.companion.di

import android.content.Context
import com.health.companion.data.local.dao.ChatMessageDao
import com.health.companion.data.local.dao.ConversationDao
import com.health.companion.data.local.dao.DocumentDao
import com.health.companion.data.local.dao.HealthMetricDao
import com.health.companion.data.local.dao.MoodEntryDao
import com.health.companion.data.remote.api.AuthApi
import com.health.companion.data.remote.api.ChatApi
import com.health.companion.data.remote.api.DashboardApi
import com.health.companion.data.remote.api.DocumentApi
import com.health.companion.data.remote.api.HealthApi
import com.health.companion.data.remote.api.ProfileApi
import com.health.companion.data.repositories.AuthRepository
import com.health.companion.data.repositories.AuthRepositoryImpl
import com.health.companion.data.repositories.ChatRepository
import com.health.companion.data.repositories.ChatRepositoryImpl
import com.health.companion.data.repositories.DashboardRepository
import com.health.companion.data.repositories.DashboardRepositoryImpl
import com.health.companion.data.repositories.DocumentRepository
import com.health.companion.data.repositories.DocumentRepositoryImpl
import com.health.companion.data.repositories.HealthRepository
import com.health.companion.data.repositories.HealthRepositoryImpl
import com.health.companion.data.repositories.ProfileRepository
import com.health.companion.data.repositories.ProfileRepositoryImpl
import com.health.companion.services.WebSocketManager
import com.health.companion.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Singleton
    @Provides
    fun provideAuthRepository(
        authApi: AuthApi,
        tokenManager: TokenManager
    ): AuthRepository = AuthRepositoryImpl(authApi, tokenManager)
    
    @Singleton
    @Provides
    fun provideChatRepository(
        chatApi: ChatApi,
        chatMessageDao: ChatMessageDao,
        conversationDao: ConversationDao,
        webSocketManager: WebSocketManager
    ): ChatRepository = ChatRepositoryImpl(chatApi, chatMessageDao, conversationDao, webSocketManager)
    
    @Singleton
    @Provides
    fun provideHealthRepository(
        healthApi: HealthApi,
        healthMetricDao: HealthMetricDao,
        moodEntryDao: MoodEntryDao
    ): HealthRepository = HealthRepositoryImpl(healthApi, healthMetricDao, moodEntryDao)
    
    @Singleton
    @Provides
    fun provideDocumentRepository(
        documentApi: DocumentApi,
        documentDao: DocumentDao,
        @ApplicationContext context: Context
    ): DocumentRepository = DocumentRepositoryImpl(documentApi, documentDao, context)

    @Singleton
    @Provides
    fun provideProfileRepository(
        profileApi: ProfileApi
    ): ProfileRepository = ProfileRepositoryImpl(profileApi)

    @Singleton
    @Provides
    fun provideDashboardRepository(
        dashboardApi: DashboardApi
    ): DashboardRepository = DashboardRepositoryImpl(dashboardApi)
}
