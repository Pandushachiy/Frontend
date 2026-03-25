package com.health.companion.presentation.screens.games

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.repositories.GamesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

@Serializable
data class GameChatMessage(
    val id: String,
    val role: String,
    val text: String
)

data class MoveAnimation(val piece: Char, val from: ChessSquare, val to: ChessSquare)
data class AnimationBatch(val moves: List<MoveAnimation>, val targetFen: String)

enum class Difficulty(val level: Int, val label: String, val elo: String) {
    LVL1(1, "Новичок", "~400"),
    LVL2(2, "Начинающий", "~800"),
    LVL3(3, "Любитель", "~1100"),
    LVL4(4, "Средний", "~1400"),
    LVL5(5, "Продвинутый", "~1700"),
    LVL6(6, "Сильный", "~1900"),
    LVL7(7, "Эксперт", "~2100"),
    LVL8(8, "Мастер", "~2400"),
    LVL9(9, "Гроссмейстер", "~2800+")
}

enum class GamePhase {
    LOADING, PLAYING, GAME_OVER
}

@HiltViewModel
class GamesViewModel @Inject constructor(
    private val gamesRepository: GamesRepository,
    @ApplicationContext context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences("chess_cache", Context.MODE_PRIVATE)

    // Restore last known state synchronously from SharedPreferences
    private val cachedFen = prefs.getString("fen", null) ?: STARTING_FEN
    private val cachedColor = if (prefs.getString("color", "white") == "black") PlayerColor.BLACK else PlayerColor.WHITE
    private val cachedDifficulty = Difficulty.entries.find { it.level == prefs.getInt("difficulty", 5) } ?: Difficulty.LVL5
    private val cachedMoveCount = prefs.getInt("move_count", 1)

    private val _currentFen = MutableStateFlow(cachedFen)
    val currentFen: StateFlow<String> = _currentFen.asStateFlow()

    private val _animQueue = MutableSharedFlow<AnimationBatch>(extraBufferCapacity = 1)
    val animQueue = _animQueue.asSharedFlow()

    private val _playerColor = MutableStateFlow(cachedColor)
    val playerColor: StateFlow<PlayerColor> = _playerColor.asStateFlow()

    private val _difficulty = MutableStateFlow(cachedDifficulty)
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()

    private val _gamePhase = MutableStateFlow(GamePhase.LOADING)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _gameResult = MutableStateFlow<GameResult?>(null)
    val gameResult: StateFlow<GameResult?> = _gameResult.asStateFlow()

    private val _gameStatus = MutableStateFlow<String?>(null)
    val gameStatus: StateFlow<String?> = _gameStatus.asStateFlow()

    private val _chatMessages = MutableStateFlow(restoreChatMessages())
    val chatMessages: StateFlow<List<GameChatMessage>> = _chatMessages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _moveCount = MutableStateFlow(cachedMoveCount)
    val moveCount: StateFlow<Int> = _moveCount.asStateFlow()

    private val _scrollToBottom = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToBottom = _scrollToBottom.asSharedFlow()

    // True when there's no active game on server → UI should show settings to start one
    private val _needsGameSetup = MutableStateFlow(false)
    val needsGameSetup: StateFlow<Boolean> = _needsGameSetup.asStateFlow()

    private var chatJob: Job? = null
    private val chatBuffer = StringBuilder()

    init {
        // Board renders instantly from cached FEN/color/difficulty
        if (cachedFen != STARTING_FEN) {
            _gamePhase.value = GamePhase.PLAYING
        }
        restoreGame()
    }

    private fun persistState() {
        prefs.edit()
            .putString("fen", _currentFen.value)
            .putString("color", if (_playerColor.value == PlayerColor.BLACK) "black" else "white")
            .putInt("difficulty", _difficulty.value.level)
            .putInt("move_count", _moveCount.value)
            .apply()
    }

    private fun restoreGame() {
        viewModelScope.launch {
            gamesRepository.getState().onSuccess { state ->
                if (state.gameId != null && state.status != "no_game") {
                    _currentFen.value = state.fen
                    _moveCount.value = state.fullmoveNumber
                    _playerColor.value = if (state.playerColor == "black") PlayerColor.BLACK else PlayerColor.WHITE
                    _difficulty.value = Difficulty.entries.find { it.level == state.difficulty } ?: Difficulty.LVL5

                    if (state.isGameOver) {
                        _gamePhase.value = GamePhase.GAME_OVER
                        _gameResult.value = resolveResult(state.status, state.result, state.playerColor)
                        _gameStatus.value = state.status
                    } else {
                        _gamePhase.value = GamePhase.PLAYING
                    }
                    persistState()
                } else {
                    _gamePhase.value = GamePhase.PLAYING
                    _needsGameSetup.value = true
                }
            }.onFailure {
                _gamePhase.value = GamePhase.PLAYING
                _needsGameSetup.value = true
                Timber.w(it, "No chess state, starting fresh")
            }
        }
    }

    fun startGame(color: PlayerColor, diff: Difficulty) {
        _playerColor.value = color
        _difficulty.value = diff
        _currentFen.value = STARTING_FEN
        _chatMessages.value = emptyList()
        persistChatMessages()
        _gameResult.value = null
        _gameStatus.value = null
        _moveCount.value = 1
        _gamePhase.value = GamePhase.PLAYING
        _needsGameSetup.value = false
        _isStreaming.value = true

        viewModelScope.launch {
            val colorStr = if (color == PlayerColor.WHITE) "white" else "black"
            gamesRepository.newGame(colorStr, diff.level).onSuccess { state ->
                val anims = mutableListOf<MoveAnimation>()
                if (state.aiMove != null) {
                    val parsed = parseUci(state.aiMove)
                    if (parsed != null) {
                        val board = parseFenBoard(STARTING_FEN)
                        val piece = board[parsed.first.rank][parsed.first.file]
                        anims.add(MoveAnimation(piece, parsed.first, parsed.second))
                    }
                }
                _animQueue.emit(AnimationBatch(anims, state.fen))
                _currentFen.value = state.fen
                _moveCount.value = state.fullmoveNumber
                _isStreaming.value = false
                persistState()
            }.onFailure {
                _isStreaming.value = false
            }
        }
    }

    fun makeMove(from: ChessSquare, to: ChessSquare, promotion: Char? = null) {
        if (_isStreaming.value) return
        val moveStr = "${from.toAlgebraic()}${to.toAlgebraic()}${promotion?.toString() ?: ""}"
        val currentBoard = parseFenBoard(_currentFen.value)
        val userPiece = currentBoard[from.rank][from.file]
        _isStreaming.value = true

        viewModelScope.launch {
            gamesRepository.makeMove(moveStr).onSuccess { resp ->
                val animations = mutableListOf<MoveAnimation>()
                animations.add(MoveAnimation(userPiece, from, to))

                if (resp.aiMove != null) {
                    val aiParsed = parseUci(resp.aiMove)
                    if (aiParsed != null) {
                        val interBoard = applyMoveToBoard(currentBoard, from, to)
                        val aiPiece = interBoard[aiParsed.first.rank][aiParsed.first.file]
                        animations.add(MoveAnimation(aiPiece, aiParsed.first, aiParsed.second))
                    }
                }

                _animQueue.emit(AnimationBatch(animations, resp.fen))
                _currentFen.value = resp.fen
                _moveCount.value = resp.fullmoveNumber

                if (resp.isGameOver) {
                    _gameResult.value = resolveResult(resp.status, resp.winner, _playerColor.value.name.lowercase())
                    _gameStatus.value = resp.status
                    _gamePhase.value = GamePhase.GAME_OVER
                }

                _isStreaming.value = false
                persistState()
            }.onFailure {
                _isStreaming.value = false
            }
        }
    }

    fun sendChatMessage(text: String) {
        if (_isStreaming.value || text.isBlank()) return
        addChatMessage("user", text)

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            _isStreaming.value = true
            _isThinking.value = true
            _streamingText.value = ""
            chatBuffer.clear()

            gamesRepository.sendChatMessage(
                message = text,
                onToken = { token ->
                    chatBuffer.append(token)
                    _streamingText.value = chatBuffer.toString()
                    _isThinking.value = false
                },
                onDone = { _ ->
                    val fullText = chatBuffer.toString().trim()
                    if (fullText.isNotBlank()) {
                        viewModelScope.launch(Dispatchers.Main) {
                            addChatMessage("assistant", fullText)
                        }
                    }
                    chatBuffer.clear()
                    viewModelScope.launch(Dispatchers.Main) {
                        _streamingText.value = ""
                        _isStreaming.value = false
                        _isThinking.value = false
                    }
                },
                onError = { errorMsg ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _streamingText.value = ""
                        addChatMessage("system", "Ошибка: $errorMsg")
                        _isStreaming.value = false
                        _isThinking.value = false
                    }
                }
            )
        }
    }

    fun undoMove() {
        if (_isStreaming.value) return
        _isStreaming.value = true

        viewModelScope.launch {
            gamesRepository.undoMove().onSuccess { resp ->
                _animQueue.emit(AnimationBatch(emptyList(), resp.fen))
                _currentFen.value = resp.fen
                _moveCount.value = resp.fullmoveNumber
                _isStreaming.value = false
                persistState()
            }.onFailure { e ->
                _isStreaming.value = false
            }
        }
    }

    fun requestHint() = sendChatMessage("Подскажи лучший ход")
    fun resign() {
        _gameResult.value = GameResult.LOSS
        _gameStatus.value = "resign"
        _gamePhase.value = GamePhase.GAME_OVER
    }

    fun newGame() {
        _gameResult.value = null
        _gameStatus.value = null
        _chatMessages.value = emptyList()
        persistChatMessages()
        _currentFen.value = STARTING_FEN
        _moveCount.value = 1
        _gamePhase.value = GamePhase.PLAYING
    }

    fun requestRematch() {
        startGame(_playerColor.value, _difficulty.value)
    }

    fun requestAnalysis() = sendChatMessage("Разбери партию, покажи ключевые моменты")
    fun showPgn() = sendChatMessage("Покажи нотацию партии")
    fun analyzeLastMove() = sendChatMessage("Подробно разбери последний ход")

    fun switchColor() {
        val newColor = if (_playerColor.value == PlayerColor.WHITE) PlayerColor.BLACK else PlayerColor.WHITE
        startGame(newColor, _difficulty.value)
    }

    fun changeDifficulty(diff: Difficulty) {
        _difficulty.value = diff
    }

    private fun addChatMessage(role: String, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        val msg = GameChatMessage(
            id = "msg_${System.currentTimeMillis()}_${(0..999).random()}",
            role = role,
            text = clean
        )
        _chatMessages.value = _chatMessages.value + msg
        persistChatMessages()
        _scrollToBottom.tryEmit(Unit)
    }

    private val chatJson = Json { ignoreUnknownKeys = true }

    private fun persistChatMessages() {
        try {
            val json = chatJson.encodeToString(_chatMessages.value.takeLast(50))
            prefs.edit().putString("chat_messages", json).apply()
        } catch (e: Exception) {
            Timber.w(e, "Failed to persist chat messages")
        }
    }

    private fun restoreChatMessages(): List<GameChatMessage> {
        return try {
            val json = prefs.getString("chat_messages", null) ?: return emptyList()
            chatJson.decodeFromString<List<GameChatMessage>>(json)
        } catch (e: Exception) {
            Timber.w(e, "Failed to restore chat messages")
            emptyList()
        }
    }

    private fun resolveResult(status: String, result: String?, playerColor: String): GameResult {
        return when (status) {
            "checkmate" -> if (result == playerColor) GameResult.WIN else GameResult.LOSS
            "stalemate", "draw" -> GameResult.DRAW
            else -> GameResult.DRAW
        }
    }
}
