package com.example.symbolmatchboard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.symbolmatchboard.data.AppSettings
import com.example.symbolmatchboard.game.model.GamePhase
import com.example.symbolmatchboard.game.model.Side
import com.example.symbolmatchboard.game.model.SymbolType
import com.example.symbolmatchboard.ui.EventKind
import com.example.symbolmatchboard.ui.GameEffect
import com.example.symbolmatchboard.ui.GameEvent
import com.example.symbolmatchboard.ui.GameUiState
import com.example.symbolmatchboard.ui.SymbolMatchViewModel
import com.example.symbolmatchboard.ui.components.AppIcons
import com.example.symbolmatchboard.ui.components.GameBoard
import com.example.symbolmatchboard.ui.components.SymbolIcon
import com.example.symbolmatchboard.ui.components.drawCpuMarker
import com.example.symbolmatchboard.ui.components.drawPlayerMarker
import com.example.symbolmatchboard.ui.theme.BoardTheme
import com.example.symbolmatchboard.util.FeedbackPlayer
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    viewModel: SymbolMatchViewModel,
    settings: AppSettings,
    onExit: () -> Unit,
    onShowResult: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val currentSettings by rememberUpdatedState(settings)
    val feedbackPlayer = remember { FeedbackPlayer() }
    DisposableEffect(Unit) { onDispose { feedbackPlayer.release() } }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GameEffect.Cue -> feedbackPlayer.play(effect.cue, view, currentSettings.sound, currentSettings.haptics)
                is GameEffect.Message -> {
                    // Replace instead of queueing so messages never pile up.
                    snackbarHostState.currentSnackbarData?.dismiss()
                    scope.launch { snackbarHostState.showSnackbar(effect.text, duration = SnackbarDuration.Short) }
                }
            }
        }
    }

    val latestOnShowResult by rememberUpdatedState(onShowResult)
    LaunchedEffect(uiState.showResult) {
        if (uiState.showResult) {
            viewModel.onResultShown()
            latestOnShowResult()
        }
    }

    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var showLeaveDialog by rememberSaveable { mutableStateOf(false) }
    val inProgress = uiState.game.phase != GamePhase.GAME_OVER &&
        uiState.game.pairsRemaining < uiState.game.cells.size / 2

    fun requestExit() {
        if (settings.confirmRestart && inProgress) showLeaveDialog = true else onExit()
    }

    BackHandler { requestExit() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            GameTopBar(
                onBack = ::requestExit,
                onRestart = {
                    if (settings.confirmRestart && inProgress) showRestartDialog = true else viewModel.startNewGame()
                },
                restartEnabled = uiState.game.phase != GamePhase.GAME_OVER
            )
            GameContent(
                uiState = uiState,
                settings = settings,
                onCellClick = viewModel::onCellTapped,
                modifier = Modifier.weight(1f)
            )
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }

    if (showRestartDialog) {
        ConfirmDialog(
            title = "Restart match?",
            text = "The current board and scores will be discarded.",
            confirmLabel = "Restart",
            onConfirm = {
                showRestartDialog = false
                viewModel.startNewGame()
            },
            onDismiss = { showRestartDialog = false }
        )
    }
    if (showLeaveDialog) {
        ConfirmDialog(
            title = "Leave match?",
            text = "This match will not be saved.",
            confirmLabel = "Leave",
            onConfirm = {
                showLeaveDialog = false
                onExit()
            },
            onDismiss = { showLeaveDialog = false }
        )
    }
}

@Composable
private fun GameTopBar(onBack: () -> Unit, onRestart: () -> Unit, restartEnabled: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(AppIcons.Back, contentDescription = "Back to home") }
        Text(
            "SYMBOL MATCH BOARD",
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        IconButton(onClick = onRestart, enabled = restartEnabled) {
            Icon(AppIcons.Restart, contentDescription = "Restart match")
        }
    }
}

@Composable
private fun GameContent(
    uiState: GameUiState,
    settings: AppSettings,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val board: @Composable (Modifier) -> Unit = { boardModifier ->
            GameBoard(
                uiState = uiState,
                showCoordinates = settings.showCoordinates,
                showTargetHighlights = settings.showTargetHighlights,
                animationsEnabled = settings.animations,
                onCellClick = onCellClick,
                modifier = boardModifier
            )
        }
        if (landscape) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                board(Modifier.weight(1f, fill = false).fillMaxSize())
                Column(
                    Modifier.width(320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Scoreboard(uiState)
                    TargetCard(uiState.game.targetSymbol, settings.animations)
                    TurnIndicator(uiState)
                    EventLine(uiState.event)
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    Modifier.readableWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Scoreboard(uiState)
                    TargetCard(uiState.game.targetSymbol, settings.animations)
                }
                board(Modifier.weight(1f).fillMaxWidth())
                Column(
                    Modifier.readableWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TurnIndicator(uiState)
                    EventLine(uiState.event)
                }
            }
        }
    }
}

private fun Modifier.readableWidth(): Modifier = widthIn(max = 560.dp).fillMaxWidth()

@Composable
private fun Scoreboard(uiState: GameUiState) {
    val game = uiState.game
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(18.dp), color = colors.surfaceContainer) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreColumn(
                label = "PLAYER",
                pairs = game.playerPairs,
                score = game.playerScore,
                color = colors.primary,
                isPlayer = true,
                active = game.activeSide == Side.PLAYER && game.phase != GamePhase.GAME_OVER,
                modifier = Modifier.weight(1f)
            )
            Column(
                Modifier.weight(1f).clearAndSetSemantics {
                    contentDescription = "${game.pairsRemaining} pairs remaining"
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PAIRS LEFT", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text("${game.pairsRemaining}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
            ScoreColumn(
                label = "CPU",
                pairs = game.cpuPairs,
                score = game.cpuScore,
                color = colors.tertiary,
                isPlayer = false,
                active = game.activeSide == Side.CPU && game.phase != GamePhase.GAME_OVER,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScoreColumn(
    label: String,
    pairs: Int,
    score: Int,
    color: Color,
    isPlayer: Boolean,
    active: Boolean,
    modifier: Modifier
) {
    Column(
        modifier.clearAndSetSemantics { contentDescription = "$label: $pairs pairs, $score points" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Canvas(Modifier.size(14.dp)) { if (isPlayer) drawPlayerMarker(color) else drawCpuMarker(color) }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("$pairs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = color)
        Text("$score pts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TargetCard(target: SymbolType?, animationsEnabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(18.dp), color = colors.secondaryContainer) {
        AnimatedContent(
            targetState = target,
            transitionSpec = {
                val duration = if (animationsEnabled) 250 else 0
                fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))
            },
            label = "target"
        ) { symbol ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .clearAndSetSemantics {
                        contentDescription = symbol?.let { "Target ${it.displayName}" } ?: "No target"
                        liveRegion = LiveRegionMode.Polite
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(shape = RoundedCornerShape(12.dp), color = colors.surfaceContainerLowest, modifier = Modifier.size(48.dp)) {
                    if (symbol != null) {
                        SymbolIcon(symbol, BoardTheme.symbolColors[symbol], Modifier.padding(6.dp))
                    }
                }
                Column {
                    Text("TARGET", style = MaterialTheme.typography.labelSmall, color = colors.onSecondaryContainer.copy(alpha = 0.75f))
                    Text(
                        symbol?.displayName?.uppercase() ?: "BOARD CLEARED",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = colors.onSecondaryContainer
                    )
                    Text(
                        if (symbol != null) "Find two matching symbols." else "Every pair has been claimed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TurnIndicator(uiState: GameUiState) {
    val game = uiState.game
    val colors = MaterialTheme.colorScheme
    val (label, sub, color) = when {
        game.phase == GamePhase.GAME_OVER -> Triple("GAME OVER", "Tallying the board...", colors.secondary)
        game.activeSide == Side.CPU -> Triple("CPU TURN", "Finding a match...", colors.tertiary)
        else -> Triple(
            "YOUR TURN",
            if (game.selectedCellIds.isEmpty()) "Tap a target symbol." else "Now tap its match.",
            colors.primary
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = when (label) {
                    "YOUR TURN" -> "Your turn"
                    "CPU TURN" -> "CPU turn"
                    else -> "Game over"
                }
                liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp)) {}
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = color)
        Spacer(Modifier.width(10.dp))
        Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun EventLine(event: GameEvent?) {
    val colors = MaterialTheme.colorScheme
    val color = when (event?.kind) {
        EventKind.PLAYER_CAPTURE -> colors.primary
        EventKind.CPU_CAPTURE -> colors.tertiary
        EventKind.MISMATCH -> colors.error
        EventKind.HINT -> colors.secondary
        else -> colors.onSurfaceVariant
    }
    Column(
        Modifier.fillMaxWidth().height(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (event != null) {
            Text(event.title, style = MaterialTheme.typography.labelLarge, color = color)
            event.detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
