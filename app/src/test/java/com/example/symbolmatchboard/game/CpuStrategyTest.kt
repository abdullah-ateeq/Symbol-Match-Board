package com.example.symbolmatchboard.game

import com.example.symbolmatchboard.game.ai.CpuProfile
import com.example.symbolmatchboard.game.ai.SymbolMatchCpuStrategy
import com.example.symbolmatchboard.game.logic.PairResolution
import com.example.symbolmatchboard.game.logic.SelectionOutcome
import com.example.symbolmatchboard.game.logic.SymbolBoardGenerator
import com.example.symbolmatchboard.game.logic.SymbolMatchGameEngine
import com.example.symbolmatchboard.game.logic.manhattanDistance
import com.example.symbolmatchboard.game.model.CellState
import com.example.symbolmatchboard.game.model.Difficulty
import com.example.symbolmatchboard.game.model.GamePhase
import com.example.symbolmatchboard.game.model.Side
import com.example.symbolmatchboard.game.model.SymbolCell
import com.example.symbolmatchboard.game.model.SymbolType
import com.example.symbolmatchboard.game.model.SymbolType.CIRCLE
import com.example.symbolmatchboard.game.model.SymbolType.MOON
import com.example.symbolmatchboard.game.model.SymbolType.STAR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CpuStrategyTest {

    /** Stars: A1 + F6 far apart, C3 + C4 adjacent. */
    private val spreadStars = board(
        "A1" to STAR, "F6" to STAR, "C3" to STAR, "C4" to STAR,
        "B1" to MOON, "E2" to MOON, "D5" to CIRCLE, "A6" to CIRCLE
    )

    private fun cellsById(cells: List<SymbolCell>) = cells.associateBy { it.id }

    private val noMistakes: (Difficulty) -> CpuProfile = { CpuProfile.forDifficulty(it).copy(mistakeChance = 0.0) }

    @Test
    fun `CPU finds the target and selects two matching symbols`() {
        val strategy = SymbolMatchCpuStrategy(Random(1), noMistakes)
        Difficulty.entries.forEach { difficulty ->
            repeat(100) {
                val move = strategy.chooseMove(spreadStars, STAR, difficulty)!!
                val byId = cellsById(spreadStars)
                assertEquals(STAR, byId.getValue(move.firstId).symbol)
                assertEquals(STAR, byId.getValue(move.secondId).symbol)
                assertTrue(move.firstId != move.secondId)
            }
        }
    }

    @Test
    fun `CPU never selects captured cells`() {
        val generator = SymbolBoardGenerator(Random(3))
        val strategy = SymbolMatchCpuStrategy(Random(4))
        repeat(300) { i ->
            val fresh = generator.generate()
            val capturedSymbols = SymbolType.entries.shuffled(Random(i)).take(4).toSet()
            val cells = fresh.map {
                if (it.symbol in capturedSymbols) it.copy(state = CellState.PLAYER_CAPTURED) else it
            }
            val target = SymbolType.entries.first { it !in capturedSymbols }
            val move = strategy.chooseMove(cells, target, Difficulty.entries[i % 3])!!
            val byId = cellsById(cells)
            assertTrue(byId.getValue(move.firstId).state.isAvailable)
            assertTrue(byId.getValue(move.secondId).state.isAvailable)
            assertEquals(target, byId.getValue(move.firstId).symbol)
        }
    }

    @Test
    fun `CPU stops when no pairs remain`() {
        val strategy = SymbolMatchCpuStrategy(Random(5))
        assertNull(strategy.chooseMove(spreadStars, null, Difficulty.HARD))
        val oneLeft = listOf(cell("A1", STAR), cell("B2", STAR, CellState.CPU_CAPTURED))
        assertNull(strategy.chooseMove(oneLeft, STAR, Difficulty.HARD))
    }

    @Test
    fun `Easy uses random pairs`() {
        val strategy = SymbolMatchCpuStrategy(Random(6), noMistakes)
        val pairs = (0 until 300).map {
            val move = strategy.chooseMove(spreadStars, STAR, Difficulty.EASY)!!
            setOf(move.firstId, move.secondId)
        }.toSet()
        // 4 stars -> 6 possible pairs; random play should explore all of them.
        assertEquals(6, pairs.size)
    }

    @Test
    fun `Easy occasionally selects a wrong second cell`() {
        val strategy = SymbolMatchCpuStrategy(Random(7))
        val byId = cellsById(spreadStars)
        val mistakes = (0 until 500).count {
            val move = strategy.chooseMove(spreadStars, STAR, Difficulty.EASY)!!
            byId.getValue(move.secondId).symbol != STAR
        }
        assertTrue("mistakes=$mistakes", mistakes in 75..225)
    }

    @Test
    fun `Normal prefers short pairs`() {
        val strategy = SymbolMatchCpuStrategy(Random(8), noMistakes)
        val shortest = setOf(idOf("C3"), idOf("C4"))
        val hits = (0 until 400).count {
            val move = strategy.chooseMove(spreadStars, STAR, Difficulty.NORMAL)!!
            setOf(move.firstId, move.secondId) == shortest
        }
        assertTrue("hits=$hits", hits > 280)
    }

    @Test
    fun `Hard always takes the shortest pair and never errs`() {
        val strategy = SymbolMatchCpuStrategy(Random(9))
        val shortest = setOf(idOf("C3"), idOf("C4"))
        repeat(300) {
            val move = strategy.chooseMove(spreadStars, STAR, Difficulty.HARD)!!
            assertEquals(shortest, setOf(move.firstId, move.secondId))
        }
    }

    @Test
    fun `Hard breaks ties by leaving the remaining copies close together`() {
        // A1-A2 and E5-E6 are adjacent; either leaves a distant pair, so compare a layout where one choice
        // isolates the leftovers: A1,A2,A3,F6. Pairs A1-A2 (leaves A3-F6: 8) vs A2-A3 (leaves A1-F6: 10).
        val cells = board("A1" to STAR, "A2" to STAR, "A3" to STAR, "F6" to STAR)
        val strategy = SymbolMatchCpuStrategy(Random(10))
        repeat(100) {
            val move = strategy.chooseMove(cells, STAR, Difficulty.HARD)!!
            assertEquals(setOf(idOf("A1"), idOf("A2")), setOf(move.firstId, move.secondId))
        }
    }

    @Test
    fun `CPU move through engine updates CPU pairs, score and keeps the turn`() {
        val engine = SymbolMatchGameEngine(Random(11))
        val strategy = SymbolMatchCpuStrategy(Random(12))
        val state = engine.createState(spreadStars, target = STAR, startingSide = Side.CPU)
        val move = strategy.chooseMove(state.cells, state.targetSymbol, Difficulty.HARD)!!
        val first = engine.select(state, move.firstId, Side.CPU) as SelectionOutcome.FirstSelected
        val pair = engine.select(first.state, move.secondId, Side.CPU) as SelectionOutcome.PairSelected
        val resolution = engine.resolvePair(pair.state) as PairResolution.Captured
        assertEquals(1, resolution.state.cpuPairs)
        assertEquals(13, resolution.state.cpuScore)
        assertEquals(1, manhattanDistance(coord("C3"), coord("C4")))
        assertEquals(GamePhase.CPU_TURN, resolution.state.phase)
        assertEquals(Side.CPU, resolution.side)
    }
}
