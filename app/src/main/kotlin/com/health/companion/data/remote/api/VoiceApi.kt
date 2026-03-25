package com.health.companion.data.remote.api

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface VoiceApi {
    
    /**
     * Get available TTS voices
     */
    @GET("voice/voices")
    suspend fun getVoices(): VoicesResponse
    
    /**
     * Speech-to-Text (STT) - transcribe audio to text
     * Uses Whisper on backend
     */
    @Multipart
    @POST("voice/transcribe")
    suspend fun transcribe(
        @Part file: MultipartBody.Part,
        @Part("language") language: RequestBody? = null
    ): TranscribeResponse
    
    /**
     * Text-to-Speech (TTS) - synthesize text to audio
     * Returns MP3 audio bytes
     */
    @POST("voice/synthesize")
    @Streaming
    suspend fun synthesize(@Body request: SynthesizeRequest): ResponseBody
    
    /**
     * Full voice chat - send audio, get AI response as audio
     * 
     * Flow: Audio → STT → AI → TTS → Audio
     * 
     * Headers in response:
     * - X-User-Text: recognized user text
     * - X-AI-Response: AI text response (URL-encoded)
     * - X-Conversation-Id: conversation UUID
     * - X-Agent-Used: which AI agent was used
     */
    @Multipart
    @POST("voice/chat")
    @Streaming
    suspend fun voiceChat(
        @Part file: MultipartBody.Part,
        @Part("conversation_id") conversationId: RequestBody? = null,
        @Part("voice") voice: RequestBody? = null,
        @Part("language") language: RequestBody? = null
    ): Response<ResponseBody>
}

@Serializable
data class VoicesResponse(
    val voices: List<VoiceInfo>,
    val default: String
)

@Serializable
data class VoiceInfo(
    val id: String,
    val name: String,
    val description: String,
    val gender: String,
    val recommended: Boolean = false
)

@Serializable
data class TranscribeResponse(
    val text: String,
    val language: String? = null,
    val audio_format: String? = null,
    val audio_size_bytes: Int? = null
)

@Serializable
data class SynthesizeRequest(
    val text: String,
    val voice: String = "nova",
    val speed: Float = 1.0f,
    val format: String = "mp3"
)
