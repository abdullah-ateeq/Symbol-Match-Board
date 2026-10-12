package com.example.symbolmatchboard.game

import com.example.symbolmatchboard.game.logic.availableTargets
import com.example.symbolmatchboard.game.logic.calculatePairScore
import com.example.symbolmatchboard.game.logic.calculatePairsRemaining
import com.example.symbolmatchboard.game.logic.chooseNextTarget
import com.example.symbolmatchboard.game.logic.determineOutcome
import com.example.symbolmatchboard.game.logic.isPerfectMatch
import com.example.symbolmatchboard.game.logic.isTarget
import com.example.symbolmatchboard.game.logic.isValidMatch
import com.example.symbolmatchboard.game.logic.manhattanDistance
import com.example.symbolmatchboard.game.model.CellState
import com.example.symbolmatchboard.game.model.GameOutcome
import com.example.symbolmatchboard.game.model.SymbolType.CIRCLE
import com.example.symbolmatchboard.game.model.SymbolType.MOON
import com.example.symbolmatchboard.game.model.SymbolType.STAR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameRulesTest {

    @Test
    fun `manhattan distance`() {
        assertEquals(1, manhattanDistance(coord("A1"), coord("A2")))
        assertEquals(4, manhattanDistance(coord("A1"), coord("C3")))
        assertEquals(10, manhattanDistance(coord("A1"), coord("F6")))
        assertEquals(manhattanDistance(coord("B5"), coord("E2")), manhattanDistance(coord("E2"), coord("B5")))
    }

    @Test
    fun `pair score is base plus distance bonus`() {
        assertEquals(13, calculatePairScore(1))
        assertEquals(12, calculatePairScore(2))
        assertEquals(12, calculatePairScore(3))
        assertEquals(11, calculatePairScore(4))
        assertEquals(11, calculatePairScore(8))
    }

    @Test
    fun `target check compares symbol`() {
        assertTrue(isTarget(cell("A1", STAR), STAR))
        assertFalse(isTarget(cell("A1", MOON), STAR))
    }

    @Test
    fun `match validation`() {
        val a1 = cell("A1", STAR)
        val b2 = cell("B2", STAR)
        assertTrue(isValidMatch(a1, b2))
        assertFalse(isValidMatch(a1, cell("C3", MOON)))
        assertFalse(isValidMatch(a1, a1))
        assertFalse(isValidMatch(a1, b2.copy(state = CellState.PLAYER_CAPTURED)))
        assertTrue(isValidMatch(a1.copy(state = CellState.SELECTED), b2))
    }

    @Test
    fun `pairs remaining counts available cells only`() {
        val cells = listOf(
            cell("A1", STAR), cell("B2", STAR),
            cell("C3", MOON, CellState.CPU_CAPTURED), cell("D4", MOON, CellState.CPU_CAPTURED)
        )
        assertEquals(1, calculatePairsRemaining(cells))
    }

    @Test
    fun `target only from symbols with at least two active cells`() {
        val cells = listOf(
            cell("A1", STAR), cell("B2", STAR),
            cell("C3", MOON, CellState.PLAYER_CAPTURED), cell("D4", MOON, CellState.PLAYER_CAPTURED),
            cell("E5", CIRCLE)
        )
        assertEquals(listOf(STAR), availableTargets(cells))
        repeat(50) { assertEquals(STAR, chooseNextTarget(cells, Random(it))) }
    }

    @Test
    fun `no target when no pairs remain`() {
        val cells = listOf(cell("A1", STAR, CellState.PLAYER_CAPTURED), cell("B2", STAR, CellState.PLAYER_CAPTURED))
        assertNull(chooseNextTarget(cells))
    }

    @Test
    fun `target selection is randomized and avoids previous when possible`() {
        val cells = board("A1" to STAR, "A2" to STAR, "B1" to MOON, "B2" to MOON, "C1" to CIRCLE, "C2" to CIRCLE)
        val seen = (0 until 100).map { chooseNextTarget(cells, Random(it)) }.toSet()
        assertEquals(setOf(STAR, MOON, CIRCLE), seen)
        repeat(100) { assertTrue(chooseNextTarget(cells, Random(it), avoid = STAR) != STAR) }
        // Falls back to the same symbol when it is the only option.
        assertEquals(STAR, chooseNextTarget(board("A1" to STAR, "A2" to STAR), Random(0), avoid = STAR))
    }

    @Test
    fun `winner by pair count, draw on equal, perfect on 18-0`() {
        assertEquals(GameOutcome.PLAYER_WIN, determineOutcome(10, 8))
        assertEquals(GameOutcome.CPU_WIN, determineOutcome(8, 10))
        assertEquals(GameOutcome.DRAW, determineOutcome(9, 9))
        assertTrue(isPerfectMatch(18, 0))
        assertFalse(isPerfectMatch(17, 1))
    }
}
