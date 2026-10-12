package com.example.symbolmatchboard.game.logic

import com.example.symbolmatchboard.game.model.CellState
import com.example.symbolmatchboard.game.model.GamePhase
import com.example.symbolmatchboard.game.model.GameResult
import com.example.symbolmatchboard.game.model.Side
import com.example.symbolmatchboard.game.model.SymbolCell
import com.example.symbolmatchboard.game.model.SymbolMatchGameState
import com.example.symbolmatchboard.game.model.SymbolType
import kotlin.random.Random

/** Result of tapping/choosing a single cell. */
sealed interface SelectionOutcome {
    /** Tap had no effect (captured cell, not this side's turn, resolution in progress). */
    data object Ignored : SelectionOutcome

    /** First pick was not the target symbol. Turn is not consumed. */
    data object WrongTarget : SelectionOutcome

    /** Same cell chosen twice. Turn is not consumed. */
    data object SameCell : SelectionOutcome

    data class FirstSelected(val state: SymbolMatchGameState) : SelectionOutcome

    /** Two cells chosen; state is now [GamePhase.RESOLVING] until [SymbolMatchGameEngine.resolvePair]. */
    data class PairSelected(
        val state: SymbolMatchGameState,
        val firstId: Int,
        val secondId: Int,
        val isMatch: Boolean
    ) : SelectionOutcome
}

sealed interface PairResolution {
    val state: SymbolMatchGameState
    val side: Side
    val firstId: Int
    val secondId: Int

    data class Captured(
        override val state: SymbolMatchGameState,
        override val side: Side,
        override val firstId: Int,
        override val secondId: Int,
        val symbol: SymbolType,
        val distance: Int,
        val points: Int
    ) : PairResolution

    data class Mismatch(
        override val state: SymbolMatchGameState,
        override val side: Side,
        override val firstId: Int,
        override val secondId: Int
    ) : PairResolution
}

/** Pure game rules. Every function takes a state and returns a new one; no UI code. */
class SymbolMatchGameEngine(
    private val random: Random = Random.Default,
    private val generator: SymbolBoardGenerator = SymbolBoardGenerator(random)
) {

    fun newGame(): SymbolMatchGameState = createState(generator.generate())

    fun createState(
        cells: List<SymbolCell>,
        target: SymbolType? = chooseNextTarget(cells, random),
        startingSide: Side = Side.PLAYER
    ): SymbolMatchGameState {
        val remaining = calculatePairsRemaining(cells)
        return SymbolMatchGameState(
            cells = cells,
            phase = when {
                remaining == 0 -> GamePhase.GAME_OVER
                startingSide == Side.PLAYER -> GamePhase.PLAYER_TURN
                else -> GamePhase.CPU_TURN
            },
            activeSide = startingSide,
            targetSymbol = if (remaining == 0) null else target,
            selectedCellIds = emptyList(),
            playerPairs = 0,
            cpuPairs = 0,
            pairsRemaining = remaining,
            playerScore = 0,
            cpuScore = 0,
            turnCount = 1,
            lastMatchDistance = null,
            lastCapturedBy = null
        )
    }

    fun select(state: SymbolMatchGameState, cellId: Int, side: Side): SelectionOutcome {
        val expectedPhase = if (side == Side.PLAYER) GamePhase.PLAYER_TURN else GamePhase.CPU_TURN
        if (state.phase != expectedPhase || state.activeSide != side) return SelectionOutcome.Ignored
        val cell = state.cell(cellId) ?: return SelectionOutcome.Ignored
        if (cell.state.isCaptured) return SelectionOutcome.Ignored
        val target = state.targetSymbol ?: return SelectionOutcome.Ignored

        val firstId = state.selectedCellIds.firstOrNull()
        if (firstId == null) {
            if (!isTarget(cell, target)) return SelectionOutcome.WrongTarget
            return SelectionOutcome.FirstSelected(
                state.copy(
                    cells = state.cells.withState(setOf(cellId), CellState.SELECTED),
                    selectedCellIds = listOf(cellId)
                )
            )
        }

        if (firstId == cellId) return SelectionOutcome.SameCell
        val first = state.cell(firstId) ?: return SelectionOutcome.Ignored
        val isMatch = isValidMatch(first, cell)
        return SelectionOutcome.PairSelected(
            state = state.copy(
                cells = state.cells.withState(setOf(cellId), CellState.SELECTED),
                selectedCellIds = listOf(firstId, cellId),
                phase = GamePhase.RESOLVING
            ),
            firstId = firstId,
            secondId = cellId,
            isMatch = isMatch
        )
    }

    /** Settles a [GamePhase.RESOLVING] state. Returns null when there is nothing to resolve. */
    fun resolvePair(state: SymbolMatchGameState): PairResolution? {
        if (state.phase != GamePhase.RESOLVING || state.selectedCellIds.size != 2) return null
        val (firstId, secondId) = state.selectedCellIds
        val first = state.cell(firstId) ?: return null
        val second = state.cell(secondId) ?: return null
        val side = state.activeSide

        if (isValidMatch(first, second)) {
            val distance = manhattanDistance(first.coordinate, second.coordinate)
            return PairResolution.Captured(
                state = capturePair(state, firstId, secondId, side),
                side = side,
                firstId = firstId,
                secondId = secondId,
                symbol = first.symbol,
                distance = distance,
                points = calculatePairScore(distance)
            )
        }

        val nextSide = side.opponent()
        val reverted = state.copy(
            cells = state.cells.withState(setOf(firstId, secondId), CellState.ACTIVE),
            selectedCellIds = emptyList(),
            activeSide = nextSide,
            phase = nextSide.turnPhase(),
            turnCount = state.turnCount + 1
        )
        return PairResolution.Mismatch(reverted, side, firstId, secondId)
    }

    /**
     * Captures a valid pair for [side]: updates ownership, pairs, score, distance and target.
     * A successful capture keeps the turn with the same side.
     */
    fun capturePair(
        state: SymbolMatchGameState,
        firstId: Int,
        secondId: Int,
        side: Side
    ): SymbolMatchGameState {
        val first = requireNotNull(state.cell(firstId)) { "Unknown cell $firstId" }
        val second = requireNotNull(state.cell(secondId)) { "Unknown cell $secondId" }
        require(isValidMatch(first, second)) { "Cells $firstId and $secondId are not a valid pair" }

        val distance = manhattanDistance(first.coordinate, second.coordinate)
        val points = calculatePairScore(distance)
        val ownerState = if (side == Side.PLAYER) CellState.PLAYER_CAPTURED else CellState.CPU_CAPTURED
        val cells = state.cells
            .withState(setOf(firstId, secondId), ownerState)
            // Any stray selection is cleared so only active/captured states remain.
            .map { if (it.state == CellState.SELECTED) it.copy(state = CellState.ACTIVE) else it }
        val remaining = calculatePairsRemaining(cells)
        val gameOver = remaining == 0

        return state.copy(
            cells = cells,
            phase = if (gameOver) GamePhase.GAME_OVER else side.turnPhase(),
            activeSide = side,
            targetSymbol = if (gameOver) null else chooseNextTarget(cells, random, avoid = first.symbol),
            selectedCellIds = emptyList(),
            playerPairs = state.playerPairs + if (side == Side.PLAYER) 1 else 0,
            cpuPairs = state.cpuPairs + if (side == Side.CPU) 1 else 0,
            pairsRemaining = remaining,
            playerScore = state.playerScore + if (side == Side.PLAYER) points else 0,
            cpuScore = state.cpuScore + if (side == Side.CPU) points else 0,
            lastMatchDistance = distance,
            lastCapturedBy = side
        )
    }

    fun result(state: SymbolMatchGameState): GameResult? {
        if (state.phase != GamePhase.GAME_OVER) return null
        val totalPairs = state.playerPairs + state.cpuPairs + state.pairsRemaining
        return GameResult(
            outcome = determineOutcome(state.playerPairs, state.cpuPairs),
            playerPairs = state.playerPairs,
            cpuPairs = state.cpuPairs,
            playerScore = state.playerScore,
            cpuScore = state.cpuScore,
            totalPairs = totalPairs,
            isPerfect = isPerfectMatch(state.playerPairs, state.cpuPairs, totalPairs)
        )
    }

    private fun List<SymbolCell>.withState(ids: Set<Int>, newState: CellState): List<SymbolCell> =
        map { if (it.id in ids) it.copy(state = newState) else it }
}

fun Side.opponent(): Side = if (this == Side.PLAYER) Side.CPU else Side.PLAYER

fun Side.turnPhase(): GamePhase = if (this == Side.PLAYER) GamePhase.PLAYER_TURN else GamePhase.CPU_TURN
