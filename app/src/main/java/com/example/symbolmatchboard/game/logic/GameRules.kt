package com.example.symbolmatchboard.game.logic

import com.example.symbolmatchboard.game.model.Coordinate
import com.example.symbolmatchboard.game.model.GameOutcome
import com.example.symbolmatchboard.game.model.SymbolCell
import com.example.symbolmatchboard.game.model.SymbolType
import com.example.symbolmatchboard.game.model.TOTAL_PAIRS
import kotlin.math.abs
import kotlin.random.Random

const val PAIR_BASE_POINTS = 10

fun manhattanDistance(first: Coordinate, second: Coordinate): Int =
    abs(first.row - second.row) + abs(first.column - second.column)

/** Base pair points plus a small distance bonus. Never used to decide the winner. */
fun calculatePairScore(distance: Int): Int {
    val bonus = when {
        distance <= 1 -> 3
        distance <= 3 -> 2
        else -> 1
    }
    return PAIR_BASE_POINTS + bonus
}

fun isTarget(cell: SymbolCell, target: SymbolType): Boolean = cell.symbol == target

fun isValidMatch(first: SymbolCell, second: SymbolCell): Boolean =
    first.state.isAvailable &&
        second.state.isAvailable &&
        first.id != second.id &&
        first.symbol == second.symbol

fun calculatePairsRemaining(cells: List<SymbolCell>): Int =
    cells.count { it.state.isAvailable } / 2

/** Symbols that still have at least two cells in play. */
fun availableTargets(cells: List<SymbolCell>): List<SymbolType> =
    cells.filter { it.state.isAvailable }
        .groupingBy { it.symbol }
        .eachCount()
        .filterValues { it >= 2 }
        .keys
        .sortedBy { it.ordinal }

/**
 * Picks a random symbol that still has a pair available. When [avoid] is given and another
 * option exists, a different symbol is chosen so the target visibly changes.
 */
fun chooseNextTarget(
    cells: List<SymbolCell>,
    random: Random = Random.Default,
    avoid: SymbolType? = null
): SymbolType? {
    val options = availableTargets(cells)
    if (options.isEmpty()) return null
    val preferred = options.filter { it != avoid }.ifEmpty { options }
    return preferred[random.nextInt(preferred.size)]
}

/** Winner is decided by captured pair count only, never by score. */
fun determineOutcome(playerPairs: Int, cpuPairs: Int): GameOutcome = when {
    playerPairs > cpuPairs -> GameOutcome.PLAYER_WIN
    cpuPairs > playerPairs -> GameOutcome.CPU_WIN
    else -> GameOutcome.DRAW
}

fun isPerfectMatch(playerPairs: Int, cpuPairs: Int, totalPairs: Int = TOTAL_PAIRS): Boolean =
    playerPairs == totalPairs && cpuPairs == 0
