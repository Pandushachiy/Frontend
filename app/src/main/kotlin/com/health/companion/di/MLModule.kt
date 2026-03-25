package com.health.companion.di

import android.content.Context
import com.health.companion.ml.camera.CameraManager
import com.health.companion.ml.voice.VoiceInputManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MLModule {

    @Singleton
    @Provides
    fun provideVoiceInputManager(
        @ApplicationContext context: Context
    ): VoiceInputManager = VoiceInputManager(context)

    @Singleton
    @Provides
    fun provideCameraManager(
        @ApplicationContext context: Context
    ): CameraManager = CameraManager(context)
}
