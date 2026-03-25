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
import com.health.companion.data.remote.api.IntelligenceApi
import com.health.companion.data.remote.api.HealthApi
import com.health.companion.data.remote.api.ProfileApi
import com.health.companion.data.remote.api.PushApi
import com.health.companion.data.repositories.AuthRepository
import com.health.companion.data.repositories.AuthRepositoryImpl
import com.health.companion.data.repositories.ChatRepository
import com.health.companion.data.repositories.ChatRepositoryImpl
import com.health.companion.data.repositories.FcmTokenRepository
import com.health.companion.data.repositories.DashboardRepository
import com.health.companion.data.repositories.DashboardRepositoryImpl
import com.health.companion.data.repositories.IntelligenceRepository
import com.health.companion.data.repositories.IntelligenceRepositoryImpl
import com.health.companion.data.repositories.DocumentRepository
import com.health.companion.data.repositories.DocumentRepositoryImpl
import com.health.companion.data.repositories.HealthRepository
import com.health.companion.data.repositories.HealthRepositoryImpl
import com.health.companion.data.repositories.ProfileRepository
import com.health.companion.data.repositories.ProfileRepositoryImpl
import com.health.companion.data.repositories.PushRepository
import com.health.companion.data.repositories.PushRepositoryImpl
import com.health.companion.data.repositories.AttachmentsRepository
import com.health.companion.data.repositories.AttachmentsRepositoryImpl
import com.health.companion.data.remote.api.AttachmentsApi
import com.health.companion.data.remote.api.LifeContextApi
import com.health.companion.data.repositories.LifeContextRepository
import com.health.companion.data.repositories.LifeContextRepositoryImpl
import com.health.companion.services.NotificationHelper
import com.health.companion.services.WebSocketManager
import com.health.companion.utils.TokenManager
import okhttp3.OkHttpClient
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
        tokenManager: TokenManager,
        fcmTokenRepository: FcmTokenRepository
    ): AuthRepository = AuthRepositoryImpl(authApi, tokenManager, fcmTokenRepository)
    
    @Singleton
    @Provides
    fun provideChatRepository(
        chatApi: ChatApi,
        chatMessageDao: ChatMessageDao,
        conversationDao: ConversationDao,
        webSocketManager: WebSocketManager,
        tokenManager: TokenManager,
        okHttpClient: OkHttpClient
    ): ChatRepository = ChatRepositoryImpl(chatApi, chatMessageDao, conversationDao, webSocketManager, tokenManager, okHttpClient)
    
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
        profileApi: ProfileApi,
        profileCache: com.health.companion.data.local.ProfileCache
    ): ProfileRepository = ProfileRepositoryImpl(profileApi, profileCache)

    @Singleton
    @Provides
    fun provideDashboardRepository(
        dashboardApi: DashboardApi
    ): DashboardRepository = DashboardRepositoryImpl(dashboardApi)

    @Singleton
    @Provides
    fun provideIntelligenceRepository(
        intelligenceApi: IntelligenceApi
    ): IntelligenceRepository = IntelligenceRepositoryImpl(intelligenceApi)

    @Singleton
    @Provides
    fun providePushRepository(
        pushApi: PushApi
    ): PushRepository = PushRepositoryImpl(pushApi)
    
    @Singleton
    @Provides
    fun provideAttachmentsRepository(
        attachmentsApi: AttachmentsApi,
        @ApplicationContext context: Context
    ): AttachmentsRepository = AttachmentsRepositoryImpl(attachmentsApi, context)
    
    @Singleton
    @Provides
    fun provideLifeContextRepository(
        lifeContextApi: LifeContextApi
    ): LifeContextRepository = LifeContextRepositoryImpl(lifeContextApi)
    
    @Singleton
    @Provides
    fun provideRemindersRepository(
        remindersApi: com.health.companion.data.remote.api.RemindersApi
    ): com.health.companion.data.repositories.RemindersRepository = 
        com.health.companion.data.repositories.RemindersRepositoryImpl(remindersApi)
    
    @Singleton
    @Provides
    fun provideNotificationHelper(
        @ApplicationContext context: Context
    ): NotificationHelper = NotificationHelper(context)
}
