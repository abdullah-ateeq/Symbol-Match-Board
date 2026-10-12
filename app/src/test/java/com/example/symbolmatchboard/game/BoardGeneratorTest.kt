package com.example.symbolmatchboard.game

import com.example.symbolmatchboard.game.logic.SymbolBoardGenerator
import com.example.symbolmatchboard.game.logic.calculatePairsRemaining
import com.example.symbolmatchboard.game.model.CellState
import com.example.symbolmatchboard.game.model.SymbolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BoardGeneratorTest {

    @Test
    fun `1000 random boards are all valid`() {
        val generator = SymbolBoardGenerator(Random(42))
        repeat(1000) {
            val cells = generator.generate()
            assertEquals(36, cells.size)
            assertEquals(9, cells.map { it.symbol }.toSet().size)
            SymbolType.entries.forEach { symbol -> assertEquals(4, cells.count { it.symbol == symbol }) }
            assertEquals(36, cells.map { it.id }.toSet().size)
            val coordinates = cells.map { it.coordinate }.toSet()
            assertEquals(36, coordinates.size)
            assertTrue(coordinates.all { it.row in 0..5 && it.column in 0..5 })
            assertTrue(cells.all { it.state == CellState.ACTIVE })
            assertEquals(18, calculatePairsRemaining(cells))
        }
    }

    @Test
    fun `boards are shuffled`() {
        val first = SymbolBoardGenerator(Random(1)).generate().map { it.symbol }
        val second = SymbolBoardGenerator(Random(2)).generate().map { it.symbol }
        assertNotEquals(first, second)
    }
}
