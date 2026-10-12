package com.example.symbolmatchboard.game

import com.example.symbolmatchboard.game.model.CellState
import com.example.symbolmatchboard.game.model.Coordinate
import com.example.symbolmatchboard.game.model.SymbolCell
import com.example.symbolmatchboard.game.model.SymbolType

/** "A1" -> column 0, row 0. */
fun coord(label: String): Coordinate = Coordinate(row = label.substring(1).toInt() - 1, column = label[0] - 'A')

fun idOf(label: String): Int = coord(label).let { it.row * 6 + it.column }

fun cell(label: String, symbol: SymbolType, state: CellState = CellState.ACTIVE) =
    SymbolCell(idOf(label), coord(label), symbol, state)

/** A small board containing only the listed cells. */
fun board(vararg cells: Pair<String, SymbolType>): List<SymbolCell> = cells.map { (label, symbol) -> cell(label, symbol) }
