package com.health.companion.data.repositories

import com.health.companion.BuildConfig
import com.health.companion.data.remote.api.*
import com.health.companion.utils.TokenManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class GamesRepository @Inject constructor(
    private val gamesApi: GamesApi,
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient
) {
    private val streamClient by lazy {
        okHttpClient.newBuilder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    suspend fun newGame(color: String, difficulty: Int): Result<ChessGameState> = runCatching {
        gamesApi.newGame(ChessNewGameRequest(color = color, difficulty = difficulty))
    }

    suspend fun makeMove(move: String): Result<ChessMoveResponse> = runCatching {
        gamesApi.makeMove(ChessMoveRequest(move = move))
    }

    suspend fun undoMove(): Result<ChessUndoResponse> = runCatching {
        gamesApi.undoMove()
    }

    suspend fun getState(): Result<ChessGameState> = runCatching {
        gamesApi.getState()
    }

    suspend fun sendChatMessage(
        message: String,
        onToken: (String) -> Unit,
        onDone: (messageId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val token = tokenManager.getAccessToken()
        if (token == null) {
            onError("Требуется авторизация")
            return
        }

        val body = JSONObject().apply {
            put("message", message)
        }.toString()

        val url = "${BuildConfig.API_BASE_URL}/games/chess/chat"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Accept-Encoding", "identity")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        suspendCancellableCoroutine { continuation ->
            val listener = object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    try {
                        val json = JSONObject(data)
                        when (json.optString("type")) {
                            "content" -> onToken(json.optString("content"))
                            "done" -> {
                                val messageId = json.optString("message_id")
                                onDone(messageId)
                                eventSource.cancel()
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                            "error" -> {
                                val msg = json.optJSONObject("error")?.optString("message")
                                    ?: json.optString("message") ?: "Ошибка"
                                onError(msg)
                                eventSource.cancel()
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Games chat SSE parse error: $data")
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    if (t?.message?.contains("Socket closed") == true) return
                    val errorMsg = when {
                        response?.code == 401 -> "Требуется авторизация"
                        response?.code == 404 -> "Эндпоинт не найден"
                        response?.code == 429 -> "Слишком много запросов"
                        response?.code == 500 -> "Ошибка сервера"
                        else -> t?.localizedMessage ?: "Ошибка подключения"
                    }
                    onError(errorMsg)
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }

            val eventSource = EventSources.createFactory(streamClient)
                .newEventSource(request, listener)

            continuation.invokeOnCancellation { eventSource.cancel() }
        }
    }
}
