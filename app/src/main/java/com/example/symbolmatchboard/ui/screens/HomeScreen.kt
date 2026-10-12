package com.example.symbolmatchboard.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.symbolmatchboard.data.AppSettings
import com.example.symbolmatchboard.data.Statistics
import com.example.symbolmatchboard.game.model.Difficulty
import com.example.symbolmatchboard.game.model.SymbolType
import com.example.symbolmatchboard.ui.components.AppIcons
import com.example.symbolmatchboard.ui.components.SectionCard
import com.example.symbolmatchboard.ui.components.StatValue
import com.example.symbolmatchboard.ui.components.SymbolIcon
import com.example.symbolmatchboard.ui.components.drawPlayerMarker
import com.example.symbolmatchboard.ui.components.drawSymbol
import com.example.symbolmatchboard.ui.theme.BoardTheme
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    statistics: Statistics,
    settings: AppSettings,
    onPlay: () -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHowToPlay by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            BrandMark(Modifier.size(84.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "SYMBOL MATCH",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text("BOARD", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Spot the pair. Claim the board.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("PLAY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }

            SectionCard(title = "DIFFICULTY") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Difficulty.entries.forEachIndexed { index, difficulty ->
                        SegmentedButton(
                            selected = settings.difficulty == difficulty,
                            onClick = { onDifficultyChange(difficulty) },
                            shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size)
                        ) { Text(difficulty.label.uppercase()) }
                    }
                }
            }

            SectionCard(title = "YOUR RECORD") {
                Row(Modifier.fillMaxWidth()) {
                    StatValue("GAMES", "${statistics.gamesPlayed}", Modifier.weight(1f))
                    StatValue("WINS", "${statistics.wins}", Modifier.weight(1f))
                    StatValue("BEST PAIRS", "${statistics.bestPairs}", Modifier.weight(1f))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth()) {
                    StatValue("BEST SCORE", "${statistics.bestScore}", Modifier.weight(1f))
                    StatValue("WIN STREAK", "${statistics.currentStreak}", Modifier.weight(1f))
                    StatValue("BEST STREAK", "${statistics.bestStreak}", Modifier.weight(1f))
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { showHowToPlay = true }) {
                    Icon(AppIcons.Help, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("How to Play")
                }
                TextButton(onClick = onOpenSettings) { Text("Settings") }
                TextButton(onClick = onOpenAbout) { Text("About") }
            }
        }
    }

    if (showHowToPlay) {
        HowToPlaySheet(onDismiss = { showHowToPlay = false })
    }
}

/** Brand mark: two matching tiles joined by a connection, the last one claimed. */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tile = colors.surfaceContainerHighest
    val accent = colors.primary
    val faded = colors.onSurfaceVariant.copy(alpha = 0.45f)
    val diamond = BoardTheme.symbolColors[SymbolType.DIAMOND]
    Canvas(modifier.clearAndSetSemantics { contentDescription = "Symbol Match Board logo" }) {
        val s = size.minDimension
        val cell = s * 0.44f
        val gap = s * 0.12f
        val positions = listOf(Offset(0f, 0f), Offset(cell + gap, 0f), Offset(0f, cell + gap), Offset(cell + gap, cell + gap))
        positions.forEachIndexed { i, pos ->
            drawRoundRect(
                color = if (i == 3) colors.primaryContainer else tile,
                topLeft = pos,
                size = Size(cell, cell),
                cornerRadius = CornerRadius(s * 0.1f)
            )
        }
        val half = cell / 2
        drawLine(accent.copy(alpha = 0.7f), positions[0] + Offset(half, half), positions[3] + Offset(half, half), s * 0.05f, StrokeCap.Round)
        drawSymbol(SymbolType.DIAMOND, diamond, positions[0] + Offset(cell * 0.15f, cell * 0.15f), cell * 0.7f)
        drawSymbol(SymbolType.STAR, faded, positions[1] + Offset(cell * 0.2f, cell * 0.2f), cell * 0.6f)
        drawSymbol(SymbolType.MOON, faded, positions[2] + Offset(cell * 0.2f, cell * 0.2f), cell * 0.6f)
        drawCircle(accent, radius = cell * 0.28f, center = positions[3] + Offset(half, half), style = Stroke(cell * 0.08f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToPlaySheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "HOW TO PLAY",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            HowToStep(SymbolType.STAR, "TARGET", "A symbol is shown at the top.")
            HowToStep(SymbolType.HEXAGON, "FIND", "Locate two matching symbols on the board.")
            HowToStep(SymbolType.DIAMOND, "CAPTURE", "Select both matching symbols to claim the pair.")
            HowToStep(SymbolType.BOLT, "KEEP PLAYING", "A successful match gives you another turn. A miss passes the turn to the CPU.")
            HowToStep(null, "WIN", "Capture more pairs than the CPU.")
            Button(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("GOT IT") }
        }
    }
}

@Composable
private fun HowToStep(symbol: SymbolType?, title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (symbol == null) colors.primaryContainer else colors.surfaceContainerHigh,
            modifier = Modifier.size(44.dp)
        ) {
            if (symbol != null) {
                SymbolIcon(symbol, BoardTheme.symbolColors[symbol], Modifier.padding(8.dp))
            } else {
                Canvas(Modifier.padding(6.dp)) { drawPlayerMarker(colors.primary) }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = colors.primary)
            Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Start)
        }
    }
}
