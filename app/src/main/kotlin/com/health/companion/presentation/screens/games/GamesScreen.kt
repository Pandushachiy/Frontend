package com.health.companion.presentation.screens.games

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.presentation.components.*
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground
import kotlinx.coroutines.launch

private enum class GameMode { CHESS, RP }

@Composable
fun GamesScreen(
    viewModel: GamesViewModel = hiltViewModel(),
    bottomPadding: Dp = 0.dp,
    onBack: () -> Unit = {}
) {
    val theme = LocalAppTheme.current
    val chatBg = LocalChatBackground.current

    val rpViewModel: RpViewModel = hiltViewModel()
    val rpPhase by rpViewModel.rpPhase.collectAsState()
    // rememberSaveable сохраняет состояние при переключении вкладок (saveState/restoreState)
    var isRpMode by rememberSaveable { mutableStateOf(false) }

    // BackHandler для RP: из Gallery → Chess, из Setup/Chat → navigateBack()
    BackHandler(enabled = isRpMode) {
        if (rpPhase == RpPhase.GALLERY) {
            isRpMode = false
        } else {
            rpViewModel.navigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(chatBg.gradient)) {
        if (!isRpMode) {
            ChessContent(
                viewModel = viewModel,
                bottomPadding = bottomPadding,
                theme = theme,
                chatBg = chatBg,
                onSwitchToRp = { isRpMode = true }
            )
        } else {
            RpScreen(
                viewModel = rpViewModel,
                bottomPadding = bottomPadding,
                onBack = { isRpMode = false }
            )
        }
    }
}

@Composable
private fun ChessContent(
    viewModel: GamesViewModel,
    bottomPadding: Dp,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    onSwitchToRp: () -> Unit
) {
    val gamePhase by viewModel.gamePhase.collectAsState()
    val currentFen by viewModel.currentFen.collectAsState()
    val playerColor by viewModel.playerColor.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()
    val moveCount by viewModel.moveCount.collectAsState()
    val gameResult by viewModel.gameResult.collectAsState()

    val gameStatus by viewModel.gameStatus.collectAsState()

    var messageInput by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    val needsGameSetup by viewModel.needsGameSetup.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val isPlaying = gamePhase == GamePhase.PLAYING || gamePhase == GamePhase.GAME_OVER
    val boardFen = currentFen

    LaunchedEffect(needsGameSetup) {
        if (needsGameSetup) showSettings = true
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    LaunchedEffect(Unit) {
        viewModel.scrollToBottom.collect {
            if (chatMessages.isNotEmpty()) chatListState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // ── TOP: CHAT (always rendered, fixed weight) ──
            Column(modifier = Modifier.fillMaxWidth().weight(0.35f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    GameChip(label = "Шахматы", selected = true, theme = theme, accentColor = theme.primary)
                    Spacer(Modifier.width(6.dp))
                    GameChip(label = "RP", selected = false, theme = theme, accentColor = theme.secondary, onClick = onSwitchToRp)
                }

                val reversedMessages = remember(chatMessages) {
                    chatMessages.filter { it.text.isNotBlank() }.reversed()
                }

                LaunchedEffect(chatMessages.size, streamingText) {
                    if (chatMessages.isNotEmpty() || streamingText.isNotBlank()) {
                        chatListState.animateScrollToItem(0)
                    }
                }

                LazyColumn(
                    state = chatListState, reverseLayout = true,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    if (streamingText.isNotBlank()) {
                        item(key = "streaming") {
                            GameChatBubble(
                                msg = GameChatMessage(id = "streaming", role = "assistant", text = streamingText),
                                theme = theme, chatBg = chatBg
                            )
                        }
                    }
                    if (isThinking) { item(key = "thinking") { ThinkingIndicator(theme = theme) } }
                    items(reversedMessages, key = { it.id }) { msg ->
                        GameChatBubble(msg = msg, theme = theme, chatBg = chatBg)
                    }
                }

                ChatInput(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    onSend = {
                        if (messageInput.isNotBlank()) { viewModel.sendChatMessage(messageInput); messageInput = "" }
                    },
                    enabled = !isStreaming, theme = theme, chatBg = chatBg
                )
            }

            // ── BOTTOM: BOARD + RESULT + CONTROLS (fixed weight) ──
            Box(modifier = Modifier.fillMaxWidth().weight(0.65f)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Spacer(Modifier.height(4.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ChessBoardSection(
                        fen = boardFen,
                        playerColor = playerColor,
                        interactive = !isStreaming && gamePhase == GamePhase.PLAYING,
                        moveCount = if (isPlaying) moveCount else 1,
                        difficulty = difficulty,
                        theme = theme, chatBg = chatBg,
                        animQueue = viewModel.animQueue,
                        onMove = { from, to, promo -> viewModel.makeMove(from, to, promo) }
                    )

                    AnimatedVisibility(
                        visible = gamePhase == GamePhase.GAME_OVER && gameResult != null,
                        enter = slideInVertically(tween(300)) { -it } + fadeIn(tween(300)),
                        exit = slideOutVertically(tween(200)) { -it } + fadeOut(tween(200))
                    ) {
                        gameResult?.let { result ->
                            GameResultBanner(
                                result = result,
                                status = gameStatus,
                                moveCount = moveCount,
                                theme = theme, chatBg = chatBg,
                                onRematch = { viewModel.requestRematch() },
                                onNewGame = { showSettings = true; viewModel.newGame() }
                            )
                        }
                    }
                    }

                    Spacer(Modifier.weight(1f))

                    ControlButtons(
                        theme = theme, chatBg = chatBg,
                        enabled = !isStreaming && gamePhase == GamePhase.PLAYING,
                        onUndo = { viewModel.undoMove() },
                        onHint = { viewModel.requestHint() },
                        onSettings = { showSettings = true }
                    )
                }
            }
        }

        // Settings overlay — full screen, outside the Column weights
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showSettings = false },
                contentAlignment = Alignment.Center
            ) {
                OnboardingCard(
                    theme = theme, chatBg = chatBg,
                    currentColor = playerColor,
                    currentDifficulty = difficulty,
                    onStart = { color, diff ->
                        showSettings = false
                        viewModel.startGame(color, diff)
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// GAME CHIP — самостоятельный блок-плашка
// ═══════════════════════════════════════════════════════════════

@Composable
private fun GameChip(
    label: String,
    selected: Boolean,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    accentColor: Color = theme.primary,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption? = null,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .then(
                if (selected) Modifier.shadow(
                    elevation = 10.dp,
                    shape = shape,
                    spotColor = accentColor.copy(alpha = 0.55f),
                    ambientColor = accentColor.copy(alpha = 0.15f)
                ) else Modifier
            )
            .clip(shape)
            .background(accentColor.copy(alpha = if (selected) 0.22f else 0.12f))
            .border(1.dp, accentColor.copy(alpha = if (selected) 0.55f else 0.30f), shape)
            .then(
                if (onClick != null) Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick
                ) else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                color = accentColor.copy(alpha = if (selected) 1f else 0.75f),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.SemiBold
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// CHESS BOARD SECTION — themed glass style
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ChessBoardSection(
    fen: String,
    playerColor: PlayerColor,
    interactive: Boolean,
    moveCount: Int,
    difficulty: Difficulty,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    animQueue: kotlinx.coroutines.flow.SharedFlow<AnimationBatch>,
    onMove: (ChessSquare, ChessSquare, Char?) -> Unit
) {
    val fenBoard = remember(fen) { parseFenBoard(fen) }
    val side = remember(fen) { extractActiveSide(fen) }
    val inCheck = remember(fen) { isKingInCheck(fenBoard, side) }
    val (capturedWhite, capturedBlack) = remember(fen) { calculateCaptured(fen) }
    val advantage = remember(fen) { materialAdvantage(fen) }

    // Initialize displayBoard from current fen to avoid a visible jump on restore.
    // The LaunchedEffect(fen) fallback and animQueue both keep it in sync during play.
    var displayBoard by remember { mutableStateOf(parseFenBoard(fen)) }
    var isAnimating by remember { mutableStateOf(false) }

    var selected by remember(fen) { mutableStateOf<ChessSquare?>(null) }
    val legalMoves = remember(fen, selected) {
        if (selected != null) getLegalMoves(fenBoard, selected!!, fen) else emptySet()
    }
    var showPromotion by remember { mutableStateOf<Pair<ChessSquare, ChessSquare>?>(null) }

    val flipped = playerColor == PlayerColor.BLACK
    val rankRange = if (flipped) (0..7) else (7 downTo 0)
    val fileRange = if (flipped) (7 downTo 0) else (0..7)

    val lightSq = theme.primary.copy(alpha = 0.10f)
    val darkSq = theme.primary.copy(alpha = 0.22f)
    val selectedCol = theme.primary.copy(alpha = 0.50f)
    val checkCol = Color(0x55FF3333)
    val legalMoveDot = theme.primary.copy(alpha = 0.45f)
    val captureRing = Color(0xAAFF4444)
    val labelCol = Color.White.copy(alpha = 0.35f)
    val borderCol = theme.primary.copy(alpha = 0.20f)

    val whitePieceColor = Color(0xFFF8F8FF)
    val whitePieceOutline = Color(0xFF1A1A2E)
    val blackPieceColor = Color(0xFF1A1020)
    val blackPieceOutline = Color(0xCCE0D0FF)

    var animPiece by remember { mutableStateOf(' ') }
    var animToSq by remember { mutableStateOf(ChessSquare(0, 0)) }
    var hiddenSq by remember { mutableStateOf<ChessSquare?>(null) }
    var capturedAnimSq by remember { mutableStateOf<ChessSquare?>(null) }
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }

    var capturedFadePiece by remember { mutableStateOf(' ') }
    var capturedFadeSq by remember { mutableStateOf(ChessSquare(0, 0)) }
    val capturedFadeAlpha = remember { Animatable(0f) }

    // Fallback: sync displayBoard when FEN changes outside animation (restore/undo)
    LaunchedEffect(fen) {
        if (!isAnimating) displayBoard = parseFenBoard(fen)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val labelW = 14.dp
            val cellSize = ((maxWidth - labelW - 2.dp) / 8).coerceAtMost(48.dp)
            val boardSize = cellSize * 8
            val cellPx = with(LocalDensity.current) { cellSize.toPx() }

            // rememberUpdatedState ensures LaunchedEffect(Unit) always reads current flipped/cellPx
            // even after recomposition (e.g. when playerColor changes from WHITE to BLACK)
            val currentFlipped by rememberUpdatedState(flipped)
            val currentCellPx by rememberUpdatedState(cellPx)

            fun sqToVisualCol(sq: ChessSquare) = if (flipped) 7 - sq.file else sq.file
            fun sqToVisualRow(sq: ChessSquare) = if (flipped) sq.rank else 7 - sq.rank

            val moveAnimSpec = tween<Float>(durationMillis = 420, easing = LinearOutSlowInEasing)
            val captureAnimSpec = tween<Float>(durationMillis = 350, easing = LinearOutSlowInEasing)

            LaunchedEffect(Unit) {
                animQueue.collect { batch ->
                    if (batch.moves.isEmpty()) {
                        displayBoard = parseFenBoard(batch.targetFen)
                        return@collect
                    }
                    for (anim in batch.moves) {
                        isAnimating = true
                        animPiece = anim.piece
                        animToSq = anim.to
                        hiddenSq = anim.from

                        val captured = displayBoard[anim.to.rank][anim.to.file]
                        if (captured != ' ') {
                            capturedAnimSq = anim.to
                            capturedFadePiece = captured
                            capturedFadeSq = anim.to
                            capturedFadeAlpha.snapTo(1f)
                            launch { capturedFadeAlpha.animateTo(0f, captureAnimSpec) }
                        }

                        // Use currentFlipped/currentCellPx — always up-to-date even after recomposition
                        val fromCol = if (currentFlipped) 7 - anim.from.file else anim.from.file
                        val fromRow = if (currentFlipped) anim.from.rank else 7 - anim.from.rank
                        val toCol   = if (currentFlipped) 7 - anim.to.file   else anim.to.file
                        val toRow   = if (currentFlipped) anim.to.rank   else 7 - anim.to.rank

                        animOffsetX.snapTo((fromCol - toCol) * currentCellPx)
                        animOffsetY.snapTo((fromRow - toRow) * currentCellPx)

                        launch { animOffsetX.animateTo(0f, moveAnimSpec) }
                        animOffsetY.animateTo(0f, moveAnimSpec)

                        displayBoard = applyMoveToBoard(displayBoard, anim.from, anim.to)
                        hiddenSq = null
                        capturedAnimSq = null
                        animPiece = ' '

                        kotlinx.coroutines.delay(350)
                    }
                    isAnimating = false
                    displayBoard = parseFenBoard(batch.targetFen)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier.width(labelW).height(boardSize),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (rank in rankRange) {
                            Box(Modifier.height(cellSize), contentAlignment = Alignment.Center) {
                                Text("${rank + 1}", style = TextStyle(fontSize = 9.sp, color = labelCol))
                            }
                        }
                    }

                    Box(modifier = Modifier.size(boardSize)) {
                        // Layer 1: Square backgrounds + highlights + markers
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(chatBg.surfaceColor.copy(alpha = 0.4f))
                                .border(1.dp, borderCol, RoundedCornerShape(4.dp))
                        ) {
                            for (rank in rankRange) {
                                Row(Modifier.height(cellSize)) {
                                    for (file in fileRange) {
                                        val sq = ChessSquare(rank, file)
                                        val piece = displayBoard[rank][file]
                                        val isLight = (rank + file) % 2 == 1
                                        val isSelected = selected == sq
                                        val isCheckSq = inCheck && piece != ' ' &&
                                                ((side == 'w' && piece == 'K') || (side == 'b' && piece == 'k'))
                                        val isLegal = sq in legalMoves
                                        val isCapture = isLegal && piece != ' '

                                        val bgColor = when {
                                            isSelected -> selectedCol
                                            isCheckSq -> checkCol
                                            isLight -> lightSq
                                            else -> darkSq
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(cellSize)
                                                .background(bgColor)
                                                .then(
                                                    if (interactive && !isAnimating) Modifier.clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        handleTap(
                                                            sq, piece, fenBoard, side, selected, legalMoves,
                                                            onSelect = { selected = it },
                                                            onMove = { from, to ->
                                                                if (isPawnPromotion(fenBoard, from, to)) {
                                                                    showPromotion = from to to
                                                                } else {
                                                                    onMove(from, to, null)
                                                                }
                                                            }
                                                        )
                                                    } else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isLegal && !isCapture) {
                                                Box(Modifier.size(cellSize * 0.28f).clip(CircleShape).background(legalMoveDot))
                                            }
                                            if (isCapture) {
                                                Box(Modifier.fillMaxSize().padding(2.dp).border(2.5.dp, captureRing, CircleShape))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Layer 2: Captured piece fade-out
                        if (capturedFadeAlpha.value > 0.01f && capturedFadePiece != ' ') {
                            val col = sqToVisualCol(capturedFadeSq)
                            val row = sqToVisualRow(capturedFadeSq)
                            Box(
                                modifier = Modifier.size(cellSize)
                                    .offset(x = cellSize * col, y = cellSize * row)
                                    .graphicsLayer { alpha = capturedFadeAlpha.value },
                                contentAlignment = Alignment.Center
                            ) {
                                StyledPiece(capturedFadePiece, cellSize, whitePieceColor, whitePieceOutline, blackPieceColor, blackPieceOutline)
                            }
                        }

                        // Layer 3: Static pieces (skip hidden and captured-during-anim)
                        for (rank in 0..7) {
                            for (file in 0..7) {
                                val piece = displayBoard[rank][file]
                                if (piece == ' ') continue
                                val sq = ChessSquare(rank, file)
                                if (sq == hiddenSq || sq == capturedAnimSq) continue
                                val col = sqToVisualCol(sq)
                                val row = sqToVisualRow(sq)
                                Box(
                                    modifier = Modifier.size(cellSize)
                                        .offset(x = cellSize * col, y = cellSize * row),
                                    contentAlignment = Alignment.Center
                                ) {
                                    StyledPiece(piece, cellSize, whitePieceColor, whitePieceOutline, blackPieceColor, blackPieceOutline)
                                }
                            }
                        }

                        // Layer 4: Moving piece (animated offset)
                        if (isAnimating && animPiece != ' ') {
                            val col = sqToVisualCol(animToSq)
                            val row = sqToVisualRow(animToSq)
                            val dx = with(LocalDensity.current) { animOffsetX.value.toDp() }
                            val dy = with(LocalDensity.current) { animOffsetY.value.toDp() }
                            Box(
                                modifier = Modifier.size(cellSize)
                                    .offset(x = cellSize * col + dx, y = cellSize * row + dy),
                                contentAlignment = Alignment.Center
                            ) {
                                StyledPiece(animPiece, cellSize, whitePieceColor, whitePieceOutline, blackPieceColor, blackPieceOutline)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.offset(x = labelW / 2).width(boardSize),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (file in fileRange) {
                        Text("${'a' + file}", style = TextStyle(fontSize = 9.sp, color = labelCol), textAlign = TextAlign.Center, modifier = Modifier.width(cellSize))
                    }
                }
            }
        }

        Spacer(Modifier.height(1.dp))

        StatusBar(side = side, moveNum = moveCount, inCheck = inCheck, difficulty = difficulty, theme = theme)
    }

    showPromotion?.let { (from, to) ->
        PromotionDialog(
            isWhite = playerColor == PlayerColor.WHITE,
            theme = theme,
            chatBg = chatBg,
            onSelect = { piece -> onMove(from, to, piece); showPromotion = null },
            onDismiss = { showPromotion = null }
        )
    }
}

@Composable
private fun StyledPiece(
    piece: Char,
    cellSize: Dp,
    whiteFill: Color,
    whiteOutline: Color,
    blackFill: Color,
    blackOutline: Color
) {
    val isWhite = isWhitePiece(piece)
    val symbol = pieceToSolid(piece)
    val pSize = (cellSize.value * 0.7f).sp
    val outlineColor = if (isWhite) whiteOutline else blackOutline
    val fillColor = if (isWhite) whiteFill else blackFill
    Box(contentAlignment = Alignment.Center) {
        for (off in listOf(
            Offset(-1.2f, 0f), Offset(1.2f, 0f),
            Offset(0f, -1.2f), Offset(0f, 1.2f),
            Offset(-0.8f, -0.8f), Offset(0.8f, -0.8f),
            Offset(-0.8f, 0.8f), Offset(0.8f, 0.8f)
        )) {
            Text(
                text = symbol, fontSize = pSize,
                color = outlineColor, textAlign = TextAlign.Center,
                modifier = Modifier.offset(x = off.x.dp, y = off.y.dp)
            )
        }
        Text(text = symbol, fontSize = pSize, color = fillColor, textAlign = TextAlign.Center)
    }
}

/** Solid (filled) unicode chess pieces — black pieces use filled glyphs, not outlined */
private fun pieceToSolid(piece: Char): String = when (piece) {
    'K' -> "♚"; 'Q' -> "♛"; 'R' -> "♜"; 'B' -> "♝"; 'N' -> "♞"; 'P' -> "♟"
    'k' -> "♚"; 'q' -> "♛"; 'r' -> "♜"; 'b' -> "♝"; 'n' -> "♞"; 'p' -> "♟"
    else -> ""
}

private fun handleTap(
    sq: ChessSquare,
    piece: Char,
    board: Array<CharArray>,
    side: Char,
    selected: ChessSquare?,
    legalMoves: Set<ChessSquare>,
    onSelect: (ChessSquare?) -> Unit,
    onMove: (ChessSquare, ChessSquare) -> Unit
) {
    if (selected == null) {
        val isOwnPiece = (side == 'w' && isWhitePiece(piece)) || (side == 'b' && isBlackPiece(piece))
        if (isOwnPiece) onSelect(sq)
    } else if (selected == sq) {
        onSelect(null)
    } else if (sq in legalMoves) {
        onMove(selected, sq)
        onSelect(null)
    } else {
        val isOwnPiece = (side == 'w' && isWhitePiece(piece)) || (side == 'b' && isBlackPiece(piece))
        if (isOwnPiece) onSelect(sq) else onSelect(null)
    }
}

// ═══════════════════════════════════════════════════════════════
// SUB-COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun CapturedPiecesRow(
    pieces: List<Char>,
    advantage: String,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    modifier: Modifier = Modifier
) {
    if (pieces.isEmpty() && advantage.isBlank()) return
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row {
            pieces.sortedByDescending { pieceValue(it) }.forEach { p ->
                Text(
                    pieceToSolid(p),
                    fontSize = 14.sp,
                    color = if (isWhitePiece(p)) Color.White.copy(alpha = 0.8f) else Color(0xFF8878AA),
                    modifier = Modifier.padding(end = 1.dp),
                    style = TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = if (isWhitePiece(p)) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.4f),
                            offset = Offset(0.5f, 0.5f), blurRadius = 1f
                        )
                    )
                )
            }
        }
        if (advantage.isNotBlank()) {
            Text(
                advantage,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.primary.copy(alpha = 0.7f))
            )
        }
    }
}

@Composable
private fun StatusBar(
    side: Char,
    moveNum: Int,
    inCheck: Boolean,
    difficulty: Difficulty,
    theme: com.health.companion.presentation.theme.AppThemeOption
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (side == 'w') Color.White else Color(0xFF555555))
                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
            Text(
                text = buildString {
                    append("Ход $moveNum · ")
                    append(if (side == 'w') "Белые" else "Чёрные")
                    if (inCheck) append("  ⚠ ШАХ!")
                },
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = if (inCheck) FontWeight.Bold else FontWeight.Medium,
                    color = if (inCheck) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.55f)
                )
            )
        }
        Text(
            text = "⚡${difficulty.label}",
            style = TextStyle(fontSize = 10.sp, color = theme.primary.copy(alpha = 0.6f))
        )
    }
}

@Composable
private fun ControlButtons(
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    enabled: Boolean,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(chatBg.surfaceColor.copy(alpha = 0.45f))
            .border(1.dp, theme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ControlBtn("Назад", Icons.AutoMirrored.Filled.Undo, theme, enabled) { onUndo() }
        ControlBtn("Подсказка", Icons.Default.Lightbulb, theme, enabled) { onHint() }
        ControlBtn("Настройки", Icons.Default.Tune, theme, true) { onSettings() }
    }
}

@Composable
private fun ControlBtn(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon, contentDescription = label,
            tint = theme.primary.copy(alpha = if (enabled) 0.85f else 0.3f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            style = TextStyle(fontSize = 9.sp, color = Color.White.copy(alpha = if (enabled) 0.6f else 0.2f))
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// CHAT BUBBLES
// ═══════════════════════════════════════════════════════════════

private fun stripMarkdown(text: String): String {
    return text
        .replace(Regex("""#{1,6}\s+"""), "")
        .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
        .replace(Regex("""__(.+?)__"""), "$1")
        .replace(Regex("""\*(.+?)\*"""), "$1")
        .replace(Regex("""_(.+?)_"""), "$1")
        .replace(Regex("""~~(.+?)~~"""), "$1")
        .replace(Regex("""`(.+?)`"""), "$1")
        .replace(Regex("""^\s*[-*+]\s+""", RegexOption.MULTILINE), "· ")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

@Composable
private fun GameChatBubble(
    msg: GameChatMessage,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption
) {
    val isUser = msg.role == "user"
    val isSystem = msg.role == "system"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isUser -> Brush.linearGradient(listOf(GlassColors.userBubble, GlassColors.userBubbleDark))
                    isSystem -> Brush.linearGradient(listOf(GlassColors.coral.copy(alpha = 0.15f), GlassColors.coral.copy(alpha = 0.08f)))
                    else -> Brush.linearGradient(listOf(chatBg.surfaceColor.copy(alpha = 0.55f), chatBg.surfaceColor.copy(alpha = 0.35f)))
                }
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        val label = if (isUser) "Вы: " else if (!isSystem) "ИИ: " else ""
        Text(
            text = label + stripMarkdown(msg.text),
            style = TextStyle(
                fontSize = 12.sp,
                color = if (isSystem) GlassColors.coral else Color.White.copy(alpha = 0.88f),
                lineHeight = 15.sp,
                fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun ThinkingIndicator(theme: com.health.companion.presentation.theme.AppThemeOption) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        val infiniteTransition = rememberInfiniteTransition(label = "thinking")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
            label = "alpha"
        )
        Row(
            modifier = Modifier
                .clip(GlassShapes.assistantBubble)
                .background(theme.primary.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(3) { i ->
                Box(
                    Modifier.size(6.dp).clip(CircleShape)
                        .background(theme.primary.copy(alpha = alpha * (0.5f + i * 0.2f)))
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CHAT INPUT
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // start=20dp aligns with board squares left (6dp container + 14dp rank labels)
            // end=6dp aligns with board squares right (6dp container padding)
            .padding(start = 20.dp, end = 6.dp, top = 2.dp, bottom = 2.dp)
            .shadow(elevation = 6.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        chatBg.inputColor.copy(alpha = 0.95f),
                        chatBg.surfaceColor.copy(alpha = 0.85f)
                    )
                )
            )
            .border(1.dp, theme.primary.copy(alpha = 0.18f), shape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value, onValueChange = onValueChange, enabled = enabled,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(fontSize = 13.sp, color = Color.White),
            cursorBrush = SolidColor(theme.primary),
            maxLines = 2,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text("Сообщение...", style = TextStyle(fontSize = 13.sp, color = Color.White.copy(alpha = 0.3f)))
                    }
                    innerTextField()
                }
            }
        )
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onSend, enabled = enabled && value.isNotBlank(), modifier = Modifier.size(26.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить",
                tint = if (value.isNotBlank()) theme.primary else Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ONBOARDING CARD (overlay on board)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OnboardingCard(
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    currentColor: PlayerColor = PlayerColor.WHITE,
    currentDifficulty: Difficulty = Difficulty.LVL5,
    onStart: (PlayerColor, Difficulty) -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }
    var selectedDiff by remember { mutableStateOf(currentDifficulty) }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(chatBg.surfaceColor.copy(alpha = 0.92f))
            .border(1.dp, theme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ШАХМАТЫ", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.primary))
        Text("Stockfish Engine", style = TextStyle(fontSize = 10.sp, color = Color.White.copy(alpha = 0.35f)))
        Spacer(Modifier.height(12.dp))

        // Color selection
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(PlayerColor.WHITE to "Белые", PlayerColor.BLACK to "Чёрные").forEach { (color, label) ->
                val sel = selectedColor == color
                Box(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) theme.primary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                        .border(1.dp, if (sel) theme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .clickable { selectedColor = color }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "♚", fontSize = 24.sp,
                            color = if (color == PlayerColor.WHITE) Color.White else Color(0xFF8878AA),
                            style = TextStyle(shadow = androidx.compose.ui.graphics.Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0.5f, 1f), blurRadius = 2f))
                        )
                        Text(label, style = TextStyle(fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) theme.primary else Color.White.copy(alpha = 0.6f)))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("Сложность:", style = TextStyle(fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)))
        Spacer(Modifier.height(6.dp))

        // 3x3 grid of difficulty levels
        for (row in 0..2) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0..2) {
                    val diff = Difficulty.entries[row * 3 + col]
                    val sel = selectedDiff == diff
                    Box(
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sel) theme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f))
                            .border(1.dp, if (sel) theme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
                            .clickable { selectedDiff = diff }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${diff.level}", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (sel) theme.primary else Color.White.copy(alpha = 0.6f)))
                            Text(diff.label, style = TextStyle(fontSize = 8.sp, color = if (sel) theme.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.35f)))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        GlassButton(onClick = { onStart(selectedColor, selectedDiff) }, modifier = Modifier.fillMaxWidth()) {
            Text("▶ Начать", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
        }
    }
}

// ColorOption removed — integrated into OnboardingCard

// ═══════════════════════════════════════════════════════════════
// GAME RESULT BANNER (compact, like Chess.com / Lichess)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun GameResultBanner(
    result: GameResult,
    status: String?,
    moveCount: Int,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    onRematch: () -> Unit,
    onNewGame: () -> Unit
) {
    val (accentColor, title, subtitle) = when (result) {
        GameResult.WIN -> Triple(
            GlassColors.success,
            "Победа!",
            when (status) {
                "checkmate" -> "Мат за $moveCount ходов"
                "resign" -> "Противник сдался"
                else -> "Вы выиграли"
            }
        )
        GameResult.LOSS -> Triple(
            GlassColors.coral,
            "Поражение",
            when (status) {
                "checkmate" -> "Мат на $moveCount ходу"
                "resign" -> "Вы сдались"
                else -> "Вы проиграли"
            }
        )
        GameResult.DRAW -> Triple(
            GlassColors.warning,
            "Ничья",
            when (status) {
                "stalemate" -> "Пат"
                else -> "Ничья на $moveCount ходу"
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.18f),
                        chatBg.surfaceColor.copy(alpha = 0.7f)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(Modifier.width(10.dp))

        // Center: result text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            )
            Text(
                subtitle,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.55f)
                )
            )
        }

        // Right: action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(
                onClick = onRematch,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Реванш",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onNewGame,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.primary.copy(alpha = 0.12f))
                    .border(1.dp, theme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Новая игра",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PROMOTION DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
private fun PromotionDialog(
    isWhite: Boolean,
    theme: com.health.companion.presentation.theme.AppThemeOption,
    chatBg: com.health.companion.presentation.theme.ChatBackgroundOption,
    onSelect: (Char) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = chatBg.surfaceColor,
        title = {
            Text("Превращение:", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
        },
        text = {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                val pieces = listOf('q' to "♛", 'r' to "♜", 'b' to "♝", 'n' to "♞")
                val pieceColor = if (isWhite) Color.White else Color(0xFFD0D0D0)
                pieces.forEach { (code, symbol) ->
                    Box(
                        modifier = Modifier.size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.primary.copy(alpha = 0.12f))
                            .border(1.dp, theme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { onSelect(code) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(symbol, fontSize = 28.sp, color = pieceColor)
                    }
                }
            }
        },
        confirmButton = {}
    )
}
