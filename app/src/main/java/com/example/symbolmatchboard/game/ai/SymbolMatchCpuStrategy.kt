package com.example.symbolmatchboard.game.ai

import com.example.symbolmatchboard.game.logic.manhattanDistance
import com.example.symbolmatchboard.game.model.Difficulty
import com.example.symbolmatchboard.game.model.SymbolCell
import com.example.symbolmatchboard.game.model.SymbolType
import kotlin.random.Random

/** The two cells the CPU will select, in order. */
data class CpuMove(val firstId: Int, val secondId: Int)

/**
 * How a difficulty behaves.
 * @param mistakeChance probability of picking a wrong second cell.
 * @param randomChoiceChance probability of ignoring distance and picking any valid pair.
 */
data class CpuProfile(
    val mistakeChance: Double,
    val randomChoiceChance: Double
) {
    companion object {
        fun forDifficulty(difficulty: Difficulty): CpuProfile = when (difficulty) {
            Difficulty.EASY -> CpuProfile(mistakeChance = 0.30, randomChoiceChance = 1.0)
            Difficulty.NORMAL -> CpuProfile(mistakeChance = 0.08, randomChoiceChance = 0.25)
            Difficulty.HARD -> CpuProfile(mistakeChance = 0.0, randomChoiceChance = 0.0)
        }
    }
}

/**
 * Lightweight CPU opponent. The board is fully visible, so the CPU legitimately sees every cell;
 * difficulty only changes how carefully it chooses.
 */
class SymbolMatchCpuStrategy(
    private val random: Random = Random.Default,
    private val profileFor: (Difficulty) -> CpuProfile = CpuProfile::forDifficulty
) {

    fun chooseMove(cells: List<SymbolCell>, target: SymbolType?, difficulty: Difficulty): CpuMove? {
        if (target == null) return null
        val active = cells.filter { it.state.isAvailable }
        val targets = active.filter { it.symbol == target }
        if (targets.size < 2) return null

        val profile = profileFor(difficulty)
        val decoys = active.filter { it.symbol != target }
        if (decoys.isNotEmpty() && random.nextDouble() < profile.mistakeChance) {
            val first = targets.random(random)
            return CpuMove(first.id, decoys.random(random).id)
        }

        val pairs = candidatePairs(targets)
        val chosen = when {
            random.nextDouble() < profile.randomChoiceChance -> pairs.random(random)
            difficulty == Difficulty.HARD -> pairs.bestBy { hardScore(it, targets) }
            else -> pairs.bestBy { it.distance }
        }
        // Randomize which of the two is tapped first so the CPU does not look mechanical.
        return if (random.nextBoolean()) CpuMove(chosen.a.id, chosen.b.id) else CpuMove(chosen.b.id, chosen.a.id)
    }

    internal data class CandidatePair(val a: SymbolCell, val b: SymbolCell) {
        val distance: Int = manhattanDistance(a.coordinate, b.coordinate)
    }

    internal fun candidatePairs(targets: List<SymbolCell>): List<CandidatePair> =
        targets.indices.flatMap { i ->
            (i + 1 until targets.size).map { j -> CandidatePair(targets[i], targets[j]) }
        }

    /**
     * Hard scoring: shortest pair first, then prefer leaving the remaining copies of this symbol
     * close together so no isolated, awkward pair is left behind.
     */
    private fun hardScore(pair: CandidatePair, targets: List<SymbolCell>): Int {
        val leftover = targets.filter { it.id != pair.a.id && it.id != pair.b.id }
        val leftoverDistance = candidatePairs(leftover).minOfOrNull { it.distance } ?: 0
        return pair.distance * 100 + leftoverDistance
    }

    private fun List<CandidatePair>.bestBy(score: (CandidatePair) -> Int): CandidatePair {
        val best = minOf(score)
        return filter { score(it) == best }.random(random)
    }
}
