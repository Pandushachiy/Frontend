package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.*

interface GamesApi {

    @POST("games/chess/new")
    suspend fun newGame(@Body body: ChessNewGameRequest): ChessGameState

    @POST("games/chess/move")
    suspend fun makeMove(@Body body: ChessMoveRequest): ChessMoveResponse

    @POST("games/chess/undo")
    suspend fun undoMove(): ChessUndoResponse

    @GET("games/chess/state")
    suspend fun getState(): ChessGameState

    @POST("games/chess/reset")
    suspend fun resetChess(): ChessGameState
}

@Serializable
data class ChessNewGameRequest(
    @SerialName("color") val color: String,
    @SerialName("difficulty") val difficulty: Int
)

@Serializable
data class ChessMoveRequest(
    @SerialName("move") val move: String
)

@Serializable
data class ChessChatRequest(
    @SerialName("message") val message: String
)

@Serializable
data class ChessGameState(
    @SerialName("game_id") val gameId: String? = null,
    @SerialName("fen") val fen: String,
    @SerialName("player_color") val playerColor: String = "white",
    @SerialName("difficulty") val difficulty: Int = 5,
    @SerialName("moves") val moves: List<ChessMoveRecord>? = null,
    @SerialName("status") val status: String = "in_progress",
    @SerialName("result") val result: String? = null,
    @SerialName("is_check") val isCheck: Boolean = false,
    @SerialName("is_checkmate") val isCheckmate: Boolean = false,
    @SerialName("is_stalemate") val isStalemate: Boolean = false,
    @SerialName("is_draw") val isDraw: Boolean = false,
    @SerialName("is_game_over") val isGameOver: Boolean = false,
    @SerialName("turn") val turn: String = "white",
    @SerialName("fullmove_number") val fullmoveNumber: Int = 1,
    @SerialName("legal_moves_count") val legalMovesCount: Int = 0,
    @SerialName("ai_move") val aiMove: String? = null,
    @SerialName("ai_move_san") val aiMoveSan: String? = null,
    @SerialName("winner") val winner: String? = null
)

@Serializable
data class ChessMoveResponse(
    @SerialName("user_move") val userMove: String? = null,
    @SerialName("user_move_san") val userMoveSan: String? = null,
    @SerialName("ai_move") val aiMove: String? = null,
    @SerialName("ai_move_san") val aiMoveSan: String? = null,
    @SerialName("fen") val fen: String,
    @SerialName("status") val status: String = "in_progress",
    @SerialName("is_check") val isCheck: Boolean = false,
    @SerialName("is_checkmate") val isCheckmate: Boolean = false,
    @SerialName("is_stalemate") val isStalemate: Boolean = false,
    @SerialName("is_draw") val isDraw: Boolean = false,
    @SerialName("is_game_over") val isGameOver: Boolean = false,
    @SerialName("turn") val turn: String = "white",
    @SerialName("fullmove_number") val fullmoveNumber: Int = 1,
    @SerialName("legal_moves_count") val legalMovesCount: Int = 0,
    @SerialName("winner") val winner: String? = null
)

@Serializable
data class ChessUndoResponse(
    @SerialName("fen") val fen: String,
    @SerialName("undone_moves") val undoneMoves: List<String> = emptyList(),
    @SerialName("moves_count") val movesCount: Int = 0,
    @SerialName("is_check") val isCheck: Boolean = false,
    @SerialName("turn") val turn: String = "white",
    @SerialName("fullmove_number") val fullmoveNumber: Int = 1,
    @SerialName("legal_moves_count") val legalMovesCount: Int = 0
)

@Serializable
data class ChessMoveRecord(
    @SerialName("uci") val uci: String,
    @SerialName("san") val san: String
)
