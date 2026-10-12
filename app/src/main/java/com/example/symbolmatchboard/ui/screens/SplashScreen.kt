package com.example.symbolmatchboard.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.symbolmatchboard.game.model.SymbolType
import com.example.symbolmatchboard.ui.components.drawSymbol
import com.example.symbolmatchboard.ui.theme.BoardTheme

private const val SPLASH_DURATION_MS = 1350
private const val GRID = 6

/** Decorative layout only — unrelated to any real game board. */
private val SplashSymbols = mapOf(
    2 to SymbolType.CIRCLE,
    7 to SymbolType.STAR,
    10 to SymbolType.HEXAGON,
    15 to SymbolType.BOLT,
    20 to SymbolType.DIAMOND,
    24 to SymbolType.TRIANGLE,
    27 to SymbolType.STAR,
    29 to SymbolType.SQUARE,
    33 to SymbolType.CROSS,
    31 to SymbolType.MOON
)
private const val LINK_A = 7
private const val LINK_B = 27

private fun phase(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val latestOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SPLASH_DURATION_MS, easing = LinearEasing))
        latestOnFinished()
    }

    val colors = MaterialTheme.colorScheme
    val symbolColors = BoardTheme.symbolColors
    Box(
        Modifier.fillMaxSize().clearAndSetSemantics { contentDescription = "Symbol Match Board" },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Canvas(Modifier.size(220.dp)) {
                val t = progress.value
                val gap = size.minDimension * 0.03f
                val cell = (size.minDimension - gap * (GRID - 1)) / GRID
                fun origin(index: Int) = Offset((index % GRID) * (cell + gap), (index / GRID) * (cell + gap))
                fun center(index: Int) = origin(index) + Offset(cell / 2, cell / 2)
                val captured = phase(t, 0.66f, 0.78f)

                for (index in 0 until GRID * GRID) {
                    val appear = phase(t, index * 0.005f, 0.2f + index * 0.005f)
                    if (appear <= 0f) continue
                    val inset = cell * (1f - appear) / 2f
                    val isLinked = index == LINK_A || index == LINK_B
                    val tileColor = if (isLinked && captured > 0f) colors.primaryContainer else colors.surfaceContainerHigh
                    drawRoundRect(
                        tileColor.copy(alpha = appear),
                        topLeft = origin(index) + Offset(inset, inset),
                        size = Size(cell - inset * 2, cell - inset * 2),
                        cornerRadius = CornerRadius(cell * 0.2f)
                    )
                }

                SplashSymbols.entries.forEachIndexed { order, (index, symbol) ->
                    val pop = phase(t, 0.18f + order * 0.025f, 0.32f + order * 0.025f)
                    val isLinked = index == LINK_A || index == LINK_B
                    val fade = if (isLinked) 1f - captured else 1f
                    if (pop > 0f && fade > 0f) {
                        val extent = cell * 0.7f * pop
                        val topLeft = center(index) - Offset(extent / 2, extent / 2)
                        drawSymbol(symbol, symbolColors[symbol].copy(alpha = pop * fade), topLeft, extent)
                    }
                }

                val link = phase(t, 0.46f, 0.64f)
                if (link > 0f && captured < 1f) {
                    val start = center(LINK_A)
                    val end = center(LINK_B)
                    drawLine(
                        colors.primary.copy(alpha = 0.8f * (1f - captured)),
                        start,
                        start + (end - start) * link,
                        strokeWidth = cell * 0.09f,
                        cap = StrokeCap.Round
                    )
                }
                if (captured > 0f) {
                    listOf(LINK_A, LINK_B).forEach { index ->
                        drawCircle(
                            colors.primary.copy(alpha = captured),
                            radius = cell * 0.26f * captured,
                            center = center(index),
                            style = Stroke(cell * 0.08f)
                        )
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    val title = phase(progress.value, 0.62f, 0.9f)
                    alpha = title
                    translationY = (1f - title) * 24f
                }
            ) {
                Text("SYMBOL", style = MaterialTheme.typography.headlineLarge)
                Text("MATCH", style = MaterialTheme.typography.headlineLarge, color = colors.primary)
                Text("BOARD", style = MaterialTheme.typography.headlineLarge)
            }
        }
    }
}
