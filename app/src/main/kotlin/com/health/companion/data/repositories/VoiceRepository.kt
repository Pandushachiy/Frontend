package com.health.companion.data.repositories

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import com.health.companion.data.remote.api.SynthesizeRequest
import com.health.companion.data.remote.api.TranscribeResponse
import com.health.companion.data.remote.api.VoiceApi
import com.health.companion.data.remote.api.VoiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full Voice System using Backend API:
 * - Record audio → Send to /voice/transcribe (STT)
 * - Send text → Get audio from /voice/synthesize (TTS)
 * - Full voice chat: audio → /voice/chat → audio response
 */
@Singleton
class VoiceRepository @Inject constructor(
    private val voiceApi: VoiceApi,
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _recordingAmplitude = MutableStateFlow(0)
    val recordingAmplitude: StateFlow<Int> = _recordingAmplitude.asStateFlow()
    
    // Available voices from backend
    private var cachedVoices: List<VoiceInfo>? = null
    private var defaultVoice: String = "nova"
    
    /**
     * Get available TTS voices from backend
     */
    suspend fun getVoices(): List<VoiceInfo> {
        return try {
            if (cachedVoices != null) {
                return cachedVoices!!
            }
            
            val response = voiceApi.getVoices()
            cachedVoices = response.voices
            defaultVoice = response.default
            response.voices
        } catch (e: Exception) {
            Timber.e(e, "Failed to get voices")
            // Return default voices if API fails
            listOf(
                VoiceInfo("nova", "Nova", "Женский голос", "female", true),
                VoiceInfo("alloy", "Alloy", "Нейтральный", "neutral"),
                VoiceInfo("echo", "Echo", "Мужской", "male"),
                VoiceInfo("onyx", "Onyx", "Глубокий мужской", "male"),
                VoiceInfo("shimmer", "Shimmer", "Мягкий женский", "female")
            )
        }
    }
    
    /**
     * Start recording audio
     */
    suspend fun startRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Create temp file for recording
            audioFile = File.createTempFile("voice_", ".m4a", context.cacheDir)
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            
            _isRecording.value = true
            Timber.d("Recording started: ${audioFile?.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start recording")
            _isRecording.value = false
            Result.failure(e)
        }
    }
    
    /**
     * Stop recording and return the audio file
     */
    suspend fun stopRecording(): Result<File> = withContext(Dispatchers.IO) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            
            val file = audioFile
            if (file != null && file.exists()) {
                Timber.d("Recording stopped: ${file.absolutePath}, size: ${file.length()}")
                Result.success(file)
            } else {
                Result.failure(Exception("Recording file not found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop recording")
            _isRecording.value = false
            Result.failure(e)
        }
    }
    
    /**
     * Cancel recording without saving
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            _isRecording.value = false
            Timber.d("Recording cancelled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cancel recording")
            _isRecording.value = false
        }
    }
    
    /**
     * Transcribe audio file to text using backend STT (Whisper)
     */
    suspend fun transcribe(audioFile: File, language: String = "ru"): Result<TranscribeResponse> {
        return try {
            val filePart = MultipartBody.Part.createFormData(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaType())
            )
            
            val languagePart = language.toRequestBody("text/plain".toMediaType())
            
            val response = voiceApi.transcribe(filePart, languagePart)
            Timber.d("Transcribed: ${response.text}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to transcribe audio")
            Result.failure(e)
        }
    }
    
    /**
     * Synthesize text to speech using backend TTS
     * Returns audio bytes (MP3)
     */
    suspend fun synthesize(
        text: String, 
        voice: String = defaultVoice,
        speed: Float = 1.0f
    ): Result<ByteArray> {
        return try {
            val response = voiceApi.synthesize(
                SynthesizeRequest(text, voice, speed)
            )
            val bytes = response.bytes()
            Timber.d("Synthesized audio: ${bytes.size} bytes")
            Result.success(bytes)
        } catch (e: Exception) {
            Timber.e(e, "Failed to synthesize speech")
            Result.failure(e)
        }
    }
    
    /**
     * Full voice chat: Record → STT → AI → TTS → Play
     * Returns both the text responses and audio
     */
    suspend fun voiceChat(
        audioFile: File,
        conversationId: String? = null,
        voice: String = defaultVoice,
        language: String = "ru"
    ): Result<VoiceChatResponse> {
        return try {
            val filePart = MultipartBody.Part.createFormData(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaType())
            )
            
            val conversationIdPart = conversationId?.toRequestBody("text/plain".toMediaType())
            val voicePart = voice.toRequestBody("text/plain".toMediaType())
            val languagePart = language.toRequestBody("text/plain".toMediaType())
            
            val response = voiceApi.voiceChat(
                file = filePart,
                conversationId = conversationIdPart,
                voice = voicePart,
                language = languagePart
            )
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("Voice chat failed: ${response.code()}"))
            }
            
            // Extract text from headers
            val userText = response.headers()["X-User-Text"]?.let { 
                URLDecoder.decode(it, "UTF-8") 
            } ?: ""
            
            val aiResponse = response.headers()["X-AI-Response"]?.let { 
                URLDecoder.decode(it, "UTF-8") 
            } ?: ""
            
            val newConversationId = response.headers()["X-Conversation-Id"] ?: conversationId
            val agentUsed = response.headers()["X-Agent-Used"] ?: "chat"
            
            // Get audio bytes
            val audioBytes = response.body()?.bytes() ?: byteArrayOf()
            
            Timber.d("Voice chat completed: user='$userText', ai='$aiResponse', audio=${audioBytes.size} bytes")
            
            Result.success(
                VoiceChatResponse(
                    userText = userText,
                    aiResponse = aiResponse,
                    conversationId = newConversationId,
                    agentUsed = agentUsed,
                    audioBytes = audioBytes
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Voice chat failed")
            Result.failure(e)
        }
    }
    
    /**
     * Play audio from bytes (MP3)
     */
    suspend fun playAudio(audioBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Save to temp file
            val tempFile = File.createTempFile("tts_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioBytes) }
            
            withContext(Dispatchers.Main) {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    setOnCompletionListener {
                        _isPlaying.value = false
                        tempFile.delete()
                    }
                    setOnErrorListener { _, what, extra ->
                        Timber.e("MediaPlayer error: what=$what, extra=$extra")
                        _isPlaying.value = false
                        true
                    }
                    prepare()
                    start()
                    _isPlaying.value = true
                }
            }
            
            Timber.d("Playing audio: ${audioBytes.size} bytes")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to play audio")
            _isPlaying.value = false
            Result.failure(e)
        }
    }
    
    /**
     * Stop playing audio
     */
    fun stopPlaying() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
    }
    
    /**
     * Cleanup resources
     */
    fun release() {
        cancelRecording()
        stopPlaying()
        audioFile?.delete()
    }
}

/**
 * Response from full voice chat
 */
data class VoiceChatResponse(
    val userText: String,
    val aiResponse: String,
    val conversationId: String?,
    val agentUsed: String,
    val audioBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as VoiceChatResponse
        return userText == other.userText && 
               aiResponse == other.aiResponse && 
               conversationId == other.conversationId
    }

    override fun hashCode(): Int {
        var result = userText.hashCode()
        result = 31 * result + aiResponse.hashCode()
        result = 31 * result + (conversationId?.hashCode() ?: 0)
        return result
    }
}
