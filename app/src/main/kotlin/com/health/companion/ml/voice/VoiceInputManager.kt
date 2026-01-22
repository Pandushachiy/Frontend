package com.health.companion.ml.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceInputManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    // Track if we WANT to be listening (user intent)
    private var wantToListen = false
    private var currentPromptText: String = "Говорите..."
    
    // Track if we're actually listening
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()
    
    private val _results = MutableSharedFlow<VoiceResult>(extraBufferCapacity = 1)
    val results: SharedFlow<VoiceResult> = _results.asSharedFlow()
    
    private val _partialResults = MutableStateFlow("")
    val partialResults: StateFlow<String> = _partialResults.asStateFlow()
    
    private val _error = MutableSharedFlow<VoiceError>(extraBufferCapacity = 1)
    val error: SharedFlow<VoiceError> = _error.asSharedFlow()
    
    private var currentLanguage: Locale = Locale("ru", "RU")
    
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Timber.d("Voice: Ready for speech")
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {
            Timber.d("Voice: Beginning of speech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            _rmsLevel.value = rmsdB
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Timber.d("Voice: End of speech")
            // Don't set _isListening to false here - wait for results or error
        }

        override fun onError(error: Int) {
            Timber.e("Voice: Recognition error: $error (${getErrorName(error)})")
            
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // User didn't speak - restart if we still want to listen
                    Timber.d("Voice: Transient error, wantToListen=$wantToListen")
                    if (wantToListen) {
                        mainHandler.postDelayed({ 
                            restartListening() 
                        }, 300)
                    } else {
                        _isListening.value = false
                    }
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    if (wantToListen) {
                        mainHandler.postDelayed({ 
                            createRecognizer()
                            restartListening() 
                        }, 1000)
                    } else {
                        _isListening.value = false
                    }
                }
                SpeechRecognizer.ERROR_CLIENT -> {
                    // Client error - often happens, just retry
                    Timber.d("Voice: Client error, retrying...")
                    if (wantToListen) {
                        mainHandler.postDelayed({ 
                            createRecognizer()
                            restartListening() 
                        }, 500)
                    } else {
                        _isListening.value = false
                    }
                }
                else -> {
                    _isListening.value = false
                    wantToListen = false
                    val voiceError = mapError(error)
                    scope.launch { _error.emit(voiceError) }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            
            if (!matches.isNullOrEmpty()) {
                val bestMatch = matches[0]
                val confidence = confidences?.getOrNull(0) ?: 0f
                
                Timber.d("Voice: Recognized: '$bestMatch' (confidence: $confidence)")
                
                scope.launch {
                    _results.emit(
                        VoiceResult(
                            text = bestMatch,
                            confidence = confidence,
                            alternatives = matches.drop(1)
                        )
                    )
                }
            }
            
            // Stop completely after getting results (message will be sent)
            wantToListen = false
            _isListening.value = false
            _partialResults.value = ""
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _partialResults.value = matches[0]
                Timber.d("Voice: Partial: '${matches[0]}'")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Timber.d("Voice: Event: $eventType")
        }
    }
    
    private fun getErrorName(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
        SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
        SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
        SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
        SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
        else -> "UNKNOWN($error)"
    }
    
    private fun mapError(error: Int): VoiceError = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> VoiceError.AudioError
        SpeechRecognizer.ERROR_CLIENT -> VoiceError.ClientError
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceError.PermissionError
        SpeechRecognizer.ERROR_NETWORK -> VoiceError.NetworkError
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceError.NetworkTimeout
        SpeechRecognizer.ERROR_NO_MATCH -> VoiceError.NoMatch
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceError.RecognizerBusy
        SpeechRecognizer.ERROR_SERVER -> VoiceError.ServerError
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceError.SpeechTimeout
        else -> VoiceError.Unknown(error)
    }

    fun initialize() {
        mainHandler.post {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Timber.e("Voice: Speech recognition not available on this device")
                return@post
            }
            
            try {
                createRecognizer()
                Timber.d("Voice: SpeechRecognizer initialized successfully")
            } catch (e: Exception) {
                Timber.e(e, "Voice: Failed to create SpeechRecognizer")
            }
        }
    }
    
    private fun createRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(recognitionListener)
        }
    }

    fun startListening(
        language: Locale = Locale("ru", "RU"),
        promptText: String = "Говорите..."
    ) {
        Timber.d("Voice: startListening called, current wantToListen=$wantToListen")
        currentLanguage = language
        currentPromptText = promptText
        wantToListen = true
        _isListening.value = true
        
        mainHandler.post {
            doStartListening()
        }
    }
    
    private fun restartListening() {
        if (!wantToListen) return
        Timber.d("Voice: Restarting listening...")
        doStartListening()
    }
    
    private fun doStartListening() {
        if (!wantToListen) {
            _isListening.value = false
            return
        }
        
        if (speechRecognizer == null) {
            Timber.d("Voice: Initializing recognizer first...")
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Timber.e("Voice: Speech recognition not available")
                wantToListen = false
                _isListening.value = false
                return
            }
            createRecognizer()
        }
        
        // Cancel any previous session to avoid ERROR_RECOGNIZER_BUSY
        speechRecognizer?.cancel()
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Extended timeouts to prevent immediate stop
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PROMPT, currentPromptText)
        }

        try {
            speechRecognizer?.startListening(intent)
            Timber.d("Voice: Started listening")
        } catch (e: Exception) {
            Timber.e(e, "Voice: Failed to start listening")
            // Try to recreate recognizer and retry once
            try {
                createRecognizer()
                speechRecognizer?.startListening(intent)
                Timber.d("Voice: Retry started listening")
            } catch (e2: Exception) {
                Timber.e(e2, "Voice: Retry also failed")
                wantToListen = false
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
        Timber.d("Voice: stopListening called")
        wantToListen = false
        
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _isListening.value = false
                _partialResults.value = ""
                Timber.d("Voice: Stopped listening")
            } catch (e: Exception) {
                Timber.e(e, "Voice: Failed to stop listening")
            }
        }
    }

    fun cancel() {
        wantToListen = false
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                _isListening.value = false
                _partialResults.value = ""
                Timber.d("Voice: Cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Voice: Failed to cancel")
            }
        }
    }

    fun destroy() {
        wantToListen = false
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
                _isListening.value = false
                Timber.d("Voice: Destroyed")
            } catch (e: Exception) {
                Timber.e(e, "Voice: Failed to destroy")
            }
        }
        scope.cancel()
    }

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
}

data class VoiceResult(
    val text: String,
    val confidence: Float,
    val alternatives: List<String> = emptyList()
)

sealed class VoiceError {
    object AudioError : VoiceError()
    object ClientError : VoiceError()
    object PermissionError : VoiceError()
    object NetworkError : VoiceError()
    object NetworkTimeout : VoiceError()
    object NoMatch : VoiceError()
    object RecognizerBusy : VoiceError()
    object ServerError : VoiceError()
    object SpeechTimeout : VoiceError()
    data class Unknown(val code: Int) : VoiceError()
    
    fun getMessage(): String = when (this) {
        is AudioError -> "Ошибка аудио"
        is ClientError -> "Ошибка клиента"
        is PermissionError -> "Нет разрешения"
        is NetworkError -> "Ошибка сети"
        is NetworkTimeout -> "Таймаут сети"
        is NoMatch -> "Речь не распознана"
        is RecognizerBusy -> "Сервис занят"
        is ServerError -> "Ошибка сервера"
        is SpeechTimeout -> "Нет речи"
        is Unknown -> "Ошибка: $code"
    }
}
