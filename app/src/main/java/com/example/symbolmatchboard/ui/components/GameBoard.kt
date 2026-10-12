package com.example.symbolmatchboard.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.example.symbolmatchboard.game.model.BOARD_SIZE
import com.example.symbolmatchboard.game.model.CellState
import com.example.symbolmatchboard.game.model.GamePhase
import com.example.symbolmatchboard.game.model.Side
import com.example.symbolmatchboard.game.model.SymbolCell
import com.example.symbolmatchboard.ui.GameUiState
import com.example.symbolmatchboard.ui.theme.BoardTheme

private val CellGap = 5.dp
private val BoardPadding = 8.dp
private val CoordinateGutter = 16.dp

/** Visual role of a cell in the current frame (derived; the model only stores ownership). */
private data class CellVisual(
    val targetHint: Boolean,
    val candidate: Boolean,
    val selectedBy: Side?,
    val mismatch: Boolean,
    val lastMatch: Boolean,
    val pulsing: Boolean
)

@Composable
fun GameBoard(
    uiState: GameUiState,
    showCoordinates: Boolean,
    showTargetHighlights: Boolean,
    animationsEnabled: Boolean,
    onCellClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val game = uiState.game
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = min(maxWidth, maxHeight)
        val gutter = if (showCoordinates) CoordinateGutter else 0.dp
        val cellSize = (side - BoardPadding * 2 - gutter - CellGap * (BOARD_SIZE - 1)) / BOARD_SIZE
        fun cellX(column: Int): Dp = gutter + (cellSize + CellGap) * column
        fun cellY(row: Int): Dp = gutter + (cellSize + CellGap) * row

        Box(
            Modifier
                .size(side)
                .clip(RoundedCornerShape(22.dp))
                .background(if (isDark) colors.surfaceContainerLow else colors.surfaceContainer)
                .padding(BoardPadding)
        ) {
            if (showCoordinates) {
                for (i in 0 until BOARD_SIZE) {
                    CoordinateLabel("${'A' + i}", Modifier.offset(x = cellX(i), y = 0.dp).size(cellSize, gutter))
                    CoordinateLabel("${i + 1}", Modifier.offset(x = 0.dp, y = cellY(i)).size(gutter, cellSize))
                }
            }

            val selectedIds = game.selectedCellIds
            val firstSelected = selectedIds.firstOrNull()?.let { game.cell(it) }
            val playerTurn = game.phase == GamePhase.PLAYER_TURN
            for (cell in game.cells) {
                val isTargetSymbol = cell.symbol == game.targetSymbol && cell.state.isAvailable
                val visual = CellVisual(
                    targetHint = showTargetHighlights && playerTurn && isTargetSymbol && firstSelected == null,
                    candidate = showTargetHighlights && playerTurn && isTargetSymbol &&
                        firstSelected != null && cell.id !in selectedIds,
                    selectedBy = if (cell.id in selectedIds) game.activeSide else null,
                    mismatch = cell.id in uiState.mismatchIds,
                    lastMatch = cell.id in uiState.lastMatchIds,
                    pulsing = cell.id in uiState.pulseIds
                )
                BoardCell(
                    cell = cell,
                    visual = visual,
                    isDark = isDark,
                    animationsEnabled = animationsEnabled,
                    onClick = { onCellClick(cell.id) },
                    modifier = Modifier
                        .offset(x = cellX(cell.coordinate.column), y = cellY(cell.coordinate.row))
                        .size(cellSize)
                )
            }

            uiState.matchLink?.let { (a, b) ->
                val first = game.cell(a)
                val second = game.cell(b)
                if (first != null && second != null) {
                    val lineColor = if (game.activeSide == Side.PLAYER) colors.primary else colors.tertiary
                    val progress = remember(a, b) { Animatable(if (animationsEnabled) 0f else 1f) }
                    LaunchedEffect(a, b) { progress.animateTo(1f, tween(220)) }
                    Canvas(Modifier.fillMaxSize()) {
                        val half = cellSize.toPx() / 2f
                        val start = Offset(cellX(first.coordinate.column).toPx() + half, cellY(first.coordinate.row).toPx() + half)
                        val end = Offset(cellX(second.coordinate.column).toPx() + half, cellY(second.coordinate.row).toPx() + half)
                        val tip = start + (end - start) * progress.value
                        drawLine(lineColor.copy(alpha = 0.75f), start, tip, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                        drawCircle(lineColor, radius = 4.dp.toPx(), center = start)
                        if (progress.value >= 1f) drawCircle(lineColor, radius = 4.dp.toPx(), center = end)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoordinateLabel(text: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun BoardCell(
    cell: SymbolCell,
    visual: CellVisual,
    isDark: Boolean,
    animationsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    val symbolColor = BoardTheme.symbolColors[cell.symbol]
    val shape = RoundedCornerShape(12.dp)

    val background = when (cell.state) {
        CellState.PLAYER_CAPTURED -> colors.primaryContainer
        CellState.CPU_CAPTURED -> colors.tertiaryContainer
        else -> if (isDark) colors.surfaceContainerHighest else colors.surfaceContainerLowest
    }
    val ownerColor = when (cell.state) {
        CellState.PLAYER_CAPTURED -> colors.primary
        CellState.CPU_CAPTURED -> colors.tertiary
        else -> Color.Transparent
    }
    val (borderColor, borderWidth) = when {
        visual.mismatch -> colors.error to 3.dp
        visual.selectedBy == Side.PLAYER -> colors.primary to 3.dp
        visual.selectedBy == Side.CPU -> colors.tertiary to 3.dp
        visual.lastMatch -> ownerColor to 3.dp
        visual.candidate -> colors.primary.copy(alpha = 0.85f) to 2.dp
        visual.targetHint -> colors.primary.copy(alpha = 0.45f) to 1.5.dp
        else -> colors.outlineVariant.copy(alpha = if (cell.state.isCaptured) 0f else 0.6f) to 1.dp
    }
    val animatedBackground by animateColorAsState(
        background,
        animationSpec = if (animationsEnabled) tween(260) else tween(0),
        label = "cellBackground"
    )
    val animatedBorder by animateColorAsState(
        borderColor,
        animationSpec = if (animationsEnabled) tween(160) else tween(0),
        label = "cellBorder"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            !animationsEnabled -> 1f
            visual.pulsing -> 1.1f
            visual.selectedBy != null -> 1.05f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 900f),
        label = "cellScale"
    )
    val shake = remember { Animatable(0f) }
    LaunchedEffect(visual.mismatch) {
        if (visual.mismatch && animationsEnabled) {
            for (target in listOf(-7f, 7f, -5f, 5f, -2f, 0f)) shake.animateTo(target, tween(50))
        } else {
            shake.snapTo(0f)
        }
    }

    val description = when (cell.state) {
        CellState.ACTIVE -> "Active ${cell.symbol.displayName} symbol at ${cell.coordinate.label}" +
            if (visual.targetHint || visual.candidate) ", target" else ""
        CellState.SELECTED -> "Selected ${cell.symbol.displayName} at ${cell.coordinate.label}"
        CellState.PLAYER_CAPTURED -> "Player captured ${cell.symbol.displayName} at ${cell.coordinate.label}"
        CellState.CPU_CAPTURED -> "CPU captured ${cell.symbol.displayName} at ${cell.coordinate.label}"
    }

    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = shake.value * density
            }
            .clip(shape)
            .background(animatedBackground)
            .border(borderWidth, animatedBorder, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (cell.state.isCaptured) null else ripple(),
                enabled = !cell.state.isCaptured,
                onClick = onClick
            )
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                if (!cell.state.isCaptured) onClick { onClick(); true }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = cell.state.isCaptured,
            transitionSpec = {
                if (animationsEnabled) {
                    (fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.6f)) togetherWith fadeOut(tween(160))
                } else {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                }
            },
            label = "cellContent"
        ) { captured ->
            if (captured) {
                Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                    if (cell.state == CellState.PLAYER_CAPTURED) drawPlayerMarker(ownerColor) else drawCpuMarker(ownerColor)
                }
            } else {
                SymbolIcon(cell.symbol, symbolColor, Modifier.fillMaxSize().padding(5.dp))
            }
        }
    }
}
