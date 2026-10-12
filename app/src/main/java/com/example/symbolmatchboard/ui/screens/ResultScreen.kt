package com.example.symbolmatchboard.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.symbolmatchboard.game.model.GameOutcome
import com.example.symbolmatchboard.game.model.GameResult
import com.example.symbolmatchboard.game.model.SymbolType
import com.example.symbolmatchboard.ui.components.SectionCard
import com.example.symbolmatchboard.ui.components.StatValue
import com.example.symbolmatchboard.ui.components.SymbolIcon
import com.example.symbolmatchboard.ui.theme.BoardTheme

fun resultTitle(result: GameResult): String = when (result.outcome) {
    GameOutcome.PLAYER_WIN -> "YOU WIN"
    GameOutcome.CPU_WIN -> "YOU LOSE"
    GameOutcome.DRAW -> "DRAW"
}

fun resultMessage(result: GameResult): String = when {
    result.isPerfect -> "Perfect board control."
    result.outcome == GameOutcome.PLAYER_WIN -> "You found more matches."
    result.outcome == GameOutcome.CPU_WIN -> "The CPU captured more pairs."
    else -> "Both sides matched evenly."
}

@Composable
fun ResultScreen(
    result: GameResult?,
    animationsEnabled: Boolean,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    BackHandler(onBack = onHome)
    if (result == null) {
        // Nothing to show (e.g. process was recreated): return home.
        LaunchedEffect(Unit) { onHome() }
        return
    }
    val colors = MaterialTheme.colorScheme
    val accent = when (result.outcome) {
        GameOutcome.PLAYER_WIN -> colors.primary
        GameOutcome.CPU_WIN -> colors.tertiary
        GameOutcome.DRAW -> colors.secondary
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (result.outcome == GameOutcome.PLAYER_WIN) {
                Celebration(animationsEnabled)
            } else {
                Spacer(Modifier.height(40.dp))
            }
            if (result.isPerfect) {
                Surface(shape = RoundedCornerShape(50), color = colors.primaryContainer) {
                    Text(
                        "PERFECT MATCH",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            Text(
                resultTitle(result),
                style = MaterialTheme.typography.displaySmall,
                color = accent,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "${result.playerPairs} — ${result.cpuPairs}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Text(
                resultMessage(result),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            SectionCard {
                Row(Modifier.fillMaxWidth()) {
                    StatValue("YOUR PAIRS", "${result.playerPairs}", Modifier.weight(1f), colors.primary)
                    StatValue("CPU PAIRS", "${result.cpuPairs}", Modifier.weight(1f), colors.tertiary)
                    StatValue("PAIRS", "${result.totalPairs}", Modifier.weight(1f))
                }
                HorizontalDivider(color = colors.outlineVariant)
                Row(Modifier.fillMaxWidth()) {
                    StatValue("YOUR SCORE", "${result.playerScore}", Modifier.weight(1f), colors.primary)
                    StatValue("CPU SCORE", "${result.cpuScore}", Modifier.weight(1f), colors.tertiary)
                }
                Text(
                    "Winner is decided by pairs. Score adds a small distance bonus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = onPlayAgain,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("PLAY AGAIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) }
            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) { Text("HOME") }
        }
    }
}

/** Restrained celebration: a short row of symbols that pop in one after another. */
@Composable
private fun Celebration(animationsEnabled: Boolean) {
    val symbols = listOf(SymbolType.STAR, SymbolType.DIAMOND, SymbolType.CIRCLE, SymbolType.HEXAGON, SymbolType.MOON)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(40.dp)) {
        symbols.forEachIndexed { index, symbol ->
            val progress = remember { Animatable(if (animationsEnabled) 0f else 1f) }
            LaunchedEffect(Unit) {
                if (animationsEnabled) progress.animateTo(1f, tween(durationMillis = 320, delayMillis = 90 * index))
            }
            SymbolIcon(
                symbol,
                BoardTheme.symbolColors[symbol],
                Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        alpha = progress.value
                        scaleX = 0.5f + 0.5f * progress.value
                        scaleY = 0.5f + 0.5f * progress.value
                        translationY = (1f - progress.value) * 24f
                    }
            )
        }
    }
}
