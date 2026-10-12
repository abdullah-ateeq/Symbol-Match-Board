package com.example.symbolmatchboard.game.logic

import com.example.symbolmatchboard.game.model.BOARD_SIZE
import com.example.symbolmatchboard.game.model.CellState
import com.example.symbolmatchboard.game.model.Coordinate
import com.example.symbolmatchboard.game.model.OCCURRENCES_PER_SYMBOL
import com.example.symbolmatchboard.game.model.SymbolCell
import com.example.symbolmatchboard.game.model.SymbolType
import kotlin.random.Random

/** Builds a shuffled 6 × 6 board where each of the 9 symbols appears exactly 4 times (18 pairs). */
class SymbolBoardGenerator(private val random: Random = Random.Default) {

    fun generate(): List<SymbolCell> {
        val symbols = SymbolType.entries
            .flatMap { symbol -> List(OCCURRENCES_PER_SYMBOL) { symbol } }
            .shuffled(random)
        return symbols.mapIndexed { index, symbol ->
            SymbolCell(
                id = index,
                coordinate = Coordinate(row = index / BOARD_SIZE, column = index % BOARD_SIZE),
                symbol = symbol,
                state = CellState.ACTIVE
            )
        }
    }
}
