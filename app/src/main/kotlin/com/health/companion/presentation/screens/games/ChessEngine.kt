package com.health.companion.presentation.screens.games

data class ChessSquare(val rank: Int, val file: Int) {
    fun toAlgebraic(): String = "${'a' + file}${rank + 1}"
}

enum class PlayerColor { WHITE, BLACK }

data class DetectedMove(val from: ChessSquare, val to: ChessSquare, val piece: Char)

enum class GameResult { WIN, LOSS, DRAW }

const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

fun parseFenBoard(fen: String): Array<CharArray> {
    val board = Array(8) { CharArray(8) { ' ' } }
    val placement = fen.trim().split(" ").firstOrNull() ?: return board
    val ranks = placement.split("/")
    for ((fenRankIdx, rankStr) in ranks.withIndex()) {
        val boardRank = 7 - fenRankIdx
        if (boardRank !in 0..7) continue
        var file = 0
        for (ch in rankStr) {
            if (ch.isDigit()) file += ch.digitToInt()
            else if (file < 8) { board[boardRank][file] = ch; file++ }
        }
    }
    return board
}

fun extractActiveSide(fen: String): Char {
    val parts = fen.trim().split(" ")
    return if (parts.size > 1) parts[1].firstOrNull() ?: 'w' else 'w'
}

fun extractMoveNumber(fen: String): Int {
    val parts = fen.trim().split(" ")
    return if (parts.size >= 6) parts[5].toIntOrNull() ?: 1 else 1
}

fun pieceToUnicode(piece: Char): String = when (piece) {
    'K' -> "♔"; 'Q' -> "♕"; 'R' -> "♖"; 'B' -> "♗"; 'N' -> "♘"; 'P' -> "♙"
    'k' -> "♚"; 'q' -> "♛"; 'r' -> "♜"; 'b' -> "♝"; 'n' -> "♞"; 'p' -> "♟"
    else -> ""
}

fun isWhitePiece(piece: Char) = piece in "KQRBNP"
fun isBlackPiece(piece: Char) = piece in "kqrbnp"

fun detectMove(oldFen: String, newFen: String): DetectedMove? {
    val oldBoard = parseFenBoard(oldFen)
    val newBoard = parseFenBoard(newFen)
    var removedFrom: ChessSquare? = null
    var addedTo: ChessSquare? = null
    var piece = ' '
    for (rank in 0..7) for (file in 0..7) {
        val old = oldBoard[rank][file]
        val new = newBoard[rank][file]
        if (old != new) {
            if (old != ' ' && (new == ' ' || new != old)) {
                if (removedFrom == null) { removedFrom = ChessSquare(rank, file); piece = old }
            }
            if (new != ' ' && old != new) addedTo = ChessSquare(rank, file)
        }
    }
    return if (removedFrom != null && addedTo != null) DetectedMove(removedFrom, addedTo, piece) else null
}

fun calculateCaptured(fen: String): Pair<List<Char>, List<Char>> {
    val board = parseFenBoard(fen)
    val allPieces = board.flatMap { it.toList() }.filter { it != ' ' }
    val fullWhite = "PPPPPPPPRRNNBBQK".toList()
    val fullBlack = "pppppppprrnnbbqk".toList()
    val currentWhite = allPieces.filter { it.isUpperCase() }.toMutableList()
    val currentBlack = allPieces.filter { it.isLowerCase() }.toMutableList()
    val capturedWhite = fullWhite.toMutableList(); currentWhite.forEach { capturedWhite.remove(it) }
    val capturedBlack = fullBlack.toMutableList(); currentBlack.forEach { capturedBlack.remove(it) }
    return Pair(capturedWhite, capturedBlack)
}

fun pieceValue(p: Char) = when (p.lowercaseChar()) {
    'q' -> 9; 'r' -> 5; 'b' -> 3; 'n' -> 3; 'p' -> 1; else -> 0
}

fun materialAdvantage(fen: String): Int {
    val (capturedWhite, capturedBlack) = calculateCaptured(fen)
    val whiteLost = capturedWhite.sumOf { pieceValue(it) }
    val blackLost = capturedBlack.sumOf { pieceValue(it) }
    return blackLost - whiteLost
}

fun isKingInCheck(board: Array<CharArray>, side: Char): Boolean {
    var kingRank = -1; var kingFile = -1
    val kingChar = if (side == 'w') 'K' else 'k'
    for (r in 0..7) for (f in 0..7) {
        if (board[r][f] == kingChar) { kingRank = r; kingFile = f }
    }
    if (kingRank == -1) return false

    val enemyIsWhite = side == 'b'
    fun isEnemy(p: Char) = if (enemyIsWhite) isWhitePiece(p) else isBlackPiece(p)
    fun at(r: Int, f: Int) = if (r in 0..7 && f in 0..7) board[r][f] else ' '

    val knightOffsets = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
    val enemyKnight = if (enemyIsWhite) 'N' else 'n'
    if (knightOffsets.any { (dr, df) -> at(kingRank + dr, kingFile + df) == enemyKnight }) return true

    val pawnDir = if (side == 'w') 1 else -1
    val enemyPawn = if (enemyIsWhite) 'P' else 'p'
    if (at(kingRank + pawnDir, kingFile - 1) == enemyPawn || at(kingRank + pawnDir, kingFile + 1) == enemyPawn) return true

    val bishopDirs = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
    val enemyBishop = if (enemyIsWhite) 'B' else 'b'
    val enemyQueen = if (enemyIsWhite) 'Q' else 'q'
    for ((dr, df) in bishopDirs) {
        var r = kingRank + dr; var f = kingFile + df
        while (r in 0..7 && f in 0..7) {
            val p = board[r][f]
            if (p != ' ') { if (p == enemyBishop || p == enemyQueen) return true; break }
            r += dr; f += df
        }
    }
    val rookDirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    val enemyRook = if (enemyIsWhite) 'R' else 'r'
    for ((dr, df) in rookDirs) {
        var r = kingRank + dr; var f = kingFile + df
        while (r in 0..7 && f in 0..7) {
            val p = board[r][f]
            if (p != ' ') { if (p == enemyRook || p == enemyQueen) return true; break }
            r += dr; f += df
        }
    }
    return false
}

fun cleanAIMessage(rawContent: String): String {
    return rawContent
        .replace(Regex("""^FEN:\s*.+$""", RegexOption.MULTILINE), "")
        .trim()
}

val FEN_PATTERN = Regex(
    """[rnbqkpRNBQKP1-8/]{15,71}\s+[wb]\s+[KQkq-]{1,4}\s+[a-h1-8-]{1,2}\s+\d+\s+\d+"""
)

fun extractFenFromText(text: String): String? {
    return FEN_PATTERN.find(text)?.value?.trim()
}

fun parseUci(uci: String): Pair<ChessSquare, ChessSquare>? {
    if (uci.length < 4) return null
    val fromFile = uci[0] - 'a'
    val fromRank = uci[1].digitToInt() - 1
    val toFile = uci[2] - 'a'
    val toRank = uci[3].digitToInt() - 1
    if (fromFile !in 0..7 || fromRank !in 0..7 || toFile !in 0..7 || toRank !in 0..7) return null
    return ChessSquare(fromRank, fromFile) to ChessSquare(toRank, toFile)
}

fun applyMoveToBoard(board: Array<CharArray>, from: ChessSquare, to: ChessSquare): Array<CharArray> {
    val copy = Array(8) { board[it].copyOf() }
    val piece = copy[from.rank][from.file]
    copy[to.rank][to.file] = piece
    copy[from.rank][from.file] = ' '
    if (piece.lowercaseChar() == 'p' && from.file != to.file && board[to.rank][to.file] == ' ') {
        copy[from.rank][to.file] = ' '
    }
    if (piece.lowercaseChar() == 'k' && kotlin.math.abs(from.file - to.file) == 2) {
        if (to.file == 6) { copy[to.rank][5] = copy[to.rank][7]; copy[to.rank][7] = ' ' }
        if (to.file == 2) { copy[to.rank][3] = copy[to.rank][0]; copy[to.rank][0] = ' ' }
    }
    return copy
}

fun isPawnPromotion(board: Array<CharArray>, from: ChessSquare, to: ChessSquare): Boolean {
    val piece = board[from.rank][from.file]
    return (piece == 'P' && to.rank == 7) || (piece == 'p' && to.rank == 0)
}

fun getLegalMoves(board: Array<CharArray>, from: ChessSquare, fen: String): Set<ChessSquare> {
    val piece = board[from.rank][from.file]
    if (piece == ' ') return emptySet()
    val side = if (isWhitePiece(piece)) 'w' else 'b'
    val pseudo = getPseudoLegalMoves(board, from, piece, side, fen)
    return pseudo.filter { to ->
        !wouldBeInCheck(board, from, to, side)
    }.toSet()
}

private fun getPseudoLegalMoves(
    board: Array<CharArray>, from: ChessSquare, piece: Char, side: Char, fen: String
): List<ChessSquare> {
    val moves = mutableListOf<ChessSquare>()
    val r = from.rank; val f = from.file
    fun inBounds(rank: Int, file: Int) = rank in 0..7 && file in 0..7
    fun isEmpty(rank: Int, file: Int) = inBounds(rank, file) && board[rank][file] == ' '
    fun isEnemy(rank: Int, file: Int): Boolean {
        if (!inBounds(rank, file)) return false
        val p = board[rank][file]
        return p != ' ' && ((side == 'w' && isBlackPiece(p)) || (side == 'b' && isWhitePiece(p)))
    }
    fun addIfValid(rank: Int, file: Int) {
        if (isEmpty(rank, file) || isEnemy(rank, file)) moves.add(ChessSquare(rank, file))
    }
    fun slide(dr: Int, df: Int) {
        var nr = r + dr; var nf = f + df
        while (inBounds(nr, nf)) {
            if (board[nr][nf] == ' ') { moves.add(ChessSquare(nr, nf)); nr += dr; nf += df }
            else { if (isEnemy(nr, nf)) moves.add(ChessSquare(nr, nf)); break }
        }
    }

    when (piece.lowercaseChar()) {
        'p' -> {
            val dir = if (side == 'w') 1 else -1
            val startRank = if (side == 'w') 1 else 6
            if (isEmpty(r + dir, f)) {
                moves.add(ChessSquare(r + dir, f))
                if (r == startRank && isEmpty(r + 2 * dir, f))
                    moves.add(ChessSquare(r + 2 * dir, f))
            }
            if (isEnemy(r + dir, f - 1)) moves.add(ChessSquare(r + dir, f - 1))
            if (isEnemy(r + dir, f + 1)) moves.add(ChessSquare(r + dir, f + 1))
            val ep = extractEnPassant(fen)
            if (ep != null) {
                if (ep.rank == r + dir && (ep.file == f - 1 || ep.file == f + 1))
                    moves.add(ep)
            }
        }
        'n' -> {
            for ((dr, df) in listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1))
                addIfValid(r + dr, f + df)
        }
        'b' -> { for ((dr, df) in listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)) slide(dr, df) }
        'r' -> { for ((dr, df) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) slide(dr, df) }
        'q' -> {
            for ((dr, df) in listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1, 1 to 0, -1 to 0, 0 to 1, 0 to -1))
                slide(dr, df)
        }
        'k' -> {
            for (dr in -1..1) for (df in -1..1) {
                if (dr == 0 && df == 0) continue
                addIfValid(r + dr, f + df)
            }
            val parts = fen.trim().split(" ")
            val castling = if (parts.size > 2) parts[2] else ""
            if (side == 'w' && r == 0 && f == 4) {
                if ('K' in castling && board[0][5] == ' ' && board[0][6] == ' ' && board[0][7] == 'R'
                    && !isKingInCheck(board, 'w')
                    && !isSquareAttacked(board, 0, 5, 'b') && !isSquareAttacked(board, 0, 6, 'b'))
                    moves.add(ChessSquare(0, 6))
                if ('Q' in castling && board[0][3] == ' ' && board[0][2] == ' ' && board[0][1] == ' ' && board[0][0] == 'R'
                    && !isKingInCheck(board, 'w')
                    && !isSquareAttacked(board, 0, 3, 'b') && !isSquareAttacked(board, 0, 2, 'b'))
                    moves.add(ChessSquare(0, 2))
            }
            if (side == 'b' && r == 7 && f == 4) {
                if ('k' in castling && board[7][5] == ' ' && board[7][6] == ' ' && board[7][7] == 'r'
                    && !isKingInCheck(board, 'b')
                    && !isSquareAttacked(board, 7, 5, 'w') && !isSquareAttacked(board, 7, 6, 'w'))
                    moves.add(ChessSquare(7, 6))
                if ('q' in castling && board[7][3] == ' ' && board[7][2] == ' ' && board[7][1] == ' ' && board[7][0] == 'r'
                    && !isKingInCheck(board, 'b')
                    && !isSquareAttacked(board, 7, 3, 'w') && !isSquareAttacked(board, 7, 2, 'w'))
                    moves.add(ChessSquare(7, 2))
            }
        }
    }
    return moves
}

private fun extractEnPassant(fen: String): ChessSquare? {
    val parts = fen.trim().split(" ")
    if (parts.size < 4 || parts[3] == "-") return null
    val ep = parts[3]
    if (ep.length != 2) return null
    val file = ep[0] - 'a'
    val rank = ep[1].digitToInt() - 1
    return if (file in 0..7 && rank in 0..7) ChessSquare(rank, file) else null
}

private fun isSquareAttacked(board: Array<CharArray>, rank: Int, file: Int, byColor: Char): Boolean {
    fun at(r: Int, f: Int) = if (r in 0..7 && f in 0..7) board[r][f] else ' '
    fun isAttacker(p: Char) = if (byColor == 'w') isWhitePiece(p) else isBlackPiece(p)

    val eKnight = if (byColor == 'w') 'N' else 'n'
    for ((dr, df) in listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1))
        if (at(rank + dr, file + df) == eKnight) return true

    val pDir = if (byColor == 'w') -1 else 1
    val ePawn = if (byColor == 'w') 'P' else 'p'
    if (at(rank + pDir, file - 1) == ePawn || at(rank + pDir, file + 1) == ePawn) return true

    val eBishop = if (byColor == 'w') 'B' else 'b'
    val eQueen = if (byColor == 'w') 'Q' else 'q'
    for ((dr, df) in listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)) {
        var r = rank + dr; var f = file + df
        while (r in 0..7 && f in 0..7) {
            val p = board[r][f]
            if (p != ' ') { if (p == eBishop || p == eQueen) return true; break }
            r += dr; f += df
        }
    }
    val eRook = if (byColor == 'w') 'R' else 'r'
    for ((dr, df) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
        var r = rank + dr; var f = file + df
        while (r in 0..7 && f in 0..7) {
            val p = board[r][f]
            if (p != ' ') { if (p == eRook || p == eQueen) return true; break }
            r += dr; f += df
        }
    }
    val eKing = if (byColor == 'w') 'K' else 'k'
    for (dr in -1..1) for (df in -1..1) {
        if (dr == 0 && df == 0) continue
        if (at(rank + dr, file + df) == eKing) return true
    }
    return false
}

private fun wouldBeInCheck(board: Array<CharArray>, from: ChessSquare, to: ChessSquare, side: Char): Boolean {
    val copy = Array(8) { board[it].copyOf() }
    val piece = copy[from.rank][from.file]
    copy[to.rank][to.file] = piece
    copy[from.rank][from.file] = ' '
    // en-passant capture
    if (piece.lowercaseChar() == 'p' && from.file != to.file && board[to.rank][to.file] == ' ') {
        copy[from.rank][to.file] = ' '
    }
    return isKingInCheck(copy, side)
}
