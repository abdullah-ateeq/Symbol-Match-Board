package com.example.symbolmatchboard.game.model

const val BOARD_SIZE = 6
const val TOTAL_CELLS = BOARD_SIZE * BOARD_SIZE
const val TOTAL_PAIRS = TOTAL_CELLS / 2
const val OCCURRENCES_PER_SYMBOL = 4

enum class SymbolType(val displayName: String) {
    CIRCLE("Circle"),
    TRIANGLE("Triangle"),
    SQUARE("Square"),
    STAR("Star"),
    DIAMOND("Diamond"),
    HEXAGON("Hexagon"),
    MOON("Moon"),
    BOLT("Bolt"),
    CROSS("Cross");

    val pluralName: String get() = "${displayName}s"
}

data class Coordinate(
    val row: Int,
    val column: Int
) {
    /** Board-game style label: columns A–F, rows 1–6 (e.g. "A1"). */
    val label: String get() = "${'A' + column}${row + 1}"
}

enum class Side {
    PLAYER,
    CPU
}

enum class CellState {
    ACTIVE,
    SELECTED,
    PLAYER_CAPTURED,
    CPU_CAPTURED;

    val isAvailable: Boolean get() = this == ACTIVE || this == SELECTED
    val isCaptured: Boolean get() = this == PLAYER_CAPTURED || this == CPU_CAPTURED
}

data class SymbolCell(
    val id: Int,
    val coordinate: Coordinate,
    val symbol: SymbolType,
    val state: CellState
)

enum class GamePhase {
    PLAYER_TURN,
    CPU_TURN,
    RESOLVING,
    GAME_OVER
}

enum class Difficulty(val label: String) {
    EASY("Easy"),
    NORMAL("Normal"),
    HARD("Hard")
}

data class SymbolMatchGameState(
    val cells: List<SymbolCell>,
    val phase: GamePhase,
    val activeSide: Side,
    val targetSymbol: SymbolType?,
    val selectedCellIds: List<Int>,
    val playerPairs: Int,
    val cpuPairs: Int,
    val pairsRemaining: Int,
    val playerScore: Int,
    val cpuScore: Int,
    val turnCount: Int,
    val lastMatchDistance: Int?,
    val lastCapturedBy: Side?
) {
    fun cell(id: Int): SymbolCell? = cells.firstOrNull { it.id == id }
}

enum class GameOutcome {
    PLAYER_WIN,
    CPU_WIN,
    DRAW
}

data class GameResult(
    val outcome: GameOutcome,
    val playerPairs: Int,
    val cpuPairs: Int,
    val playerScore: Int,
    val cpuScore: Int,
    val totalPairs: Int,
    val isPerfect: Boolean
)
