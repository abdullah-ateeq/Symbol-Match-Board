package com.example.symbolmatchboard.game

import com.example.symbolmatchboard.game.ai.SymbolMatchCpuStrategy
import com.example.symbolmatchboard.game.logic.PairResolution
import com.example.symbolmatchboard.game.logic.SelectionOutcome
import com.example.symbolmatchboard.game.logic.SymbolMatchGameEngine
import com.example.symbolmatchboard.game.model.CellState
import com.example.symbolmatchboard.game.model.Difficulty
import com.example.symbolmatchboard.game.model.GameOutcome
import com.example.symbolmatchboard.game.model.GamePhase
import com.example.symbolmatchboard.game.model.Side
import com.example.symbolmatchboard.game.model.SymbolMatchGameState
import com.example.symbolmatchboard.game.model.SymbolType.MOON
import com.example.symbolmatchboard.game.model.SymbolType.STAR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {
    private val engine = SymbolMatchGameEngine(Random(7))

    /** A1, B2 stars; C3, D4 moons; E5, F6 stars. Target STAR. */
    private fun starBoard(side: Side = Side.PLAYER): SymbolMatchGameState = engine.createState(
        board("A1" to STAR, "B2" to STAR, "C3" to MOON, "D4" to MOON, "E5" to STAR, "F6" to STAR),
        target = STAR,
        startingSide = side
    )

    private fun select(state: SymbolMatchGameState, label: String, side: Side = Side.PLAYER) =
        engine.select(state, idOf(label), side)

    private fun pick(state: SymbolMatchGameState, first: String, second: String, side: Side = Side.PLAYER): SelectionOutcome.PairSelected {
        val firstOutcome = select(state, first, side) as SelectionOutcome.FirstSelected
        return select(firstOutcome.state, second, side) as SelectionOutcome.PairSelected
    }

    @Test
    fun `new game starts with player, 18 pairs and a valid target`() {
        val state = engine.newGame()
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
        assertEquals(Side.PLAYER, state.activeSide)
        assertEquals(18, state.pairsRemaining)
        assertEquals(0, state.playerPairs + state.cpuPairs)
        assertNotNull(state.targetSymbol)
    }

    @Test
    fun `first selection of target marks cell selected`() {
        val outcome = select(starBoard(), "A1") as SelectionOutcome.FirstSelected
        assertEquals(listOf(idOf("A1")), outcome.state.selectedCellIds)
        assertEquals(CellState.SELECTED, outcome.state.cell(idOf("A1"))!!.state)
        assertEquals(GamePhase.PLAYER_TURN, outcome.state.phase)
    }

    @Test
    fun `wrong first selection does not consume the turn`() {
        val state = starBoard()
        assertSame(SelectionOutcome.WrongTarget, select(state, "C3"))
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
        assertTrue(state.selectedCellIds.isEmpty())
    }

    @Test
    fun `matching pair is captured and player keeps turn`() {
        val pair = pick(starBoard(), "A1", "B2")
        assertTrue(pair.isMatch)
        assertEquals(GamePhase.RESOLVING, pair.state.phase)
        val resolution = engine.resolvePair(pair.state) as PairResolution.Captured
        val state = resolution.state
        assertEquals(1, state.playerPairs)
        assertEquals(0, state.cpuPairs)
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
        assertEquals(Side.PLAYER, state.activeSide)
        assertEquals(CellState.PLAYER_CAPTURED, state.cell(idOf("A1"))!!.state)
        assertEquals(CellState.PLAYER_CAPTURED, state.cell(idOf("B2"))!!.state)
        assertEquals(2, resolution.distance)
        assertEquals(12, state.playerScore)
        assertEquals(2, state.lastMatchDistance)
        assertEquals(Side.PLAYER, state.lastCapturedBy)
        assertEquals(2, state.pairsRemaining)
        assertTrue(state.selectedCellIds.isEmpty())
    }

    @Test
    fun `mismatch passes turn to CPU and keeps target`() {
        val pair = pick(starBoard(), "A1", "C3")
        assertFalse(pair.isMatch)
        val resolution = engine.resolvePair(pair.state) as PairResolution.Mismatch
        val state = resolution.state
        assertEquals(GamePhase.CPU_TURN, state.phase)
        assertEquals(Side.CPU, state.activeSide)
        assertEquals(0, state.playerPairs)
        assertEquals(3, state.pairsRemaining)
        assertEquals(STAR, state.targetSymbol)
        assertTrue(state.cells.all { it.state == CellState.ACTIVE })
        assertEquals(2, state.turnCount)
    }

    @Test
    fun `same cell twice is rejected without consuming turn`() {
        val first = select(starBoard(), "A1") as SelectionOutcome.FirstSelected
        assertSame(SelectionOutcome.SameCell, select(first.state, "A1"))
    }

    @Test
    fun `captured cells are ignored`() {
        val captured = (engine.resolvePair(pick(starBoard(), "A1", "B2").state) as PairResolution.Captured).state
        val withStarTarget = captured.copy(targetSymbol = STAR)
        assertSame(SelectionOutcome.Ignored, select(withStarTarget, "A1"))
        val first = select(withStarTarget, "E5") as SelectionOutcome.FirstSelected
        assertSame(SelectionOutcome.Ignored, select(first.state, "B2"))
    }

    @Test
    fun `final pair ends the game`() {
        val cells = listOf(
            cell("A1", STAR), cell("B2", STAR),
            cell("C3", MOON, CellState.CPU_CAPTURED), cell("D4", MOON, CellState.CPU_CAPTURED)
        )
        val state = engine.createState(cells, target = STAR)
        val end = (engine.resolvePair(pick(state, "A1", "B2").state) as PairResolution.Captured).state
        assertEquals(0, end.pairsRemaining)
        assertEquals(GamePhase.GAME_OVER, end.phase)
        assertNull(end.targetSymbol)
    }

    @Test
    fun `CPU capture updates CPU pairs and score and keeps CPU turn`() {
        val pair = pick(starBoard(Side.CPU), "E5", "F6", Side.CPU)
        val state = (engine.resolvePair(pair.state) as PairResolution.Captured).state
        assertEquals(1, state.cpuPairs)
        assertEquals(12, state.cpuScore) // E5 → F6 is distance 2
        assertEquals(0, state.playerPairs)
        assertEquals(GamePhase.CPU_TURN, state.phase)
        assertEquals(CellState.CPU_CAPTURED, state.cell(idOf("E5"))!!.state)
    }

    @Test
    fun `CPU mismatch returns turn to player`() {
        val pair = pick(starBoard(Side.CPU), "A1", "D4", Side.CPU)
        val state = engine.resolvePair(pair.state)!!.state
        assertEquals(GamePhase.PLAYER_TURN, state.phase)
        assertEquals(Side.PLAYER, state.activeSide)
    }

    @Test
    fun `rapid taps are ignored while resolving and on the other side's turn`() {
        val pair = pick(starBoard(), "A1", "B2")
        assertSame(SelectionOutcome.Ignored, select(pair.state, "E5"))
        val resolved = engine.resolvePair(pair.state)!!
        // A second resolution of the already-resolved state is a no-op — no duplicate scoring.
        assertNull(engine.resolvePair(resolved.state))
        assertSame(SelectionOutcome.Ignored, select(starBoard(Side.CPU), "A1", Side.PLAYER))
        assertSame(SelectionOutcome.Ignored, select(starBoard(Side.PLAYER), "A1", Side.CPU))
    }

    @Test
    fun `capturePair rejects invalid pairs`() {
        val state = starBoard()
        val error = runCatching { engine.capturePair(state, idOf("A1"), idOf("C3"), Side.PLAYER) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun `result outcomes - win, loss, draw, perfect`() {
        val over = starBoard().copy(phase = GamePhase.GAME_OVER, pairsRemaining = 0)
        val win = engine.result(over.copy(playerPairs = 10, cpuPairs = 8, playerScore = 118, cpuScore = 96))!!
        assertEquals(GameOutcome.PLAYER_WIN, win.outcome)
        assertEquals(18, win.totalPairs)
        assertFalse(win.isPerfect)
        assertEquals(GameOutcome.CPU_WIN, engine.result(over.copy(playerPairs = 8, cpuPairs = 10))!!.outcome)
        val draw = engine.result(over.copy(playerPairs = 9, cpuPairs = 9))!!
        assertEquals(GameOutcome.DRAW, draw.outcome)
        val perfect = engine.result(over.copy(playerPairs = 18, cpuPairs = 0))!!
        assertTrue(perfect.isPerfect)
        assertEquals(GameOutcome.PLAYER_WIN, perfect.outcome)
        assertNull(engine.result(starBoard()))
    }

    @Test
    fun `score never decides winner`() {
        val over = starBoard().copy(phase = GamePhase.GAME_OVER, pairsRemaining = 0)
        val result = engine.result(over.copy(playerPairs = 9, cpuPairs = 9, playerScore = 117, cpuScore = 99))!!
        assertEquals(GameOutcome.DRAW, result.outcome)
    }

    @Test
    fun `restart produces a fresh independent game`() {
        val first = engine.newGame()
        val firstTarget = first.targetSymbol!!
        val targetId = first.cells.first { it.symbol == firstTarget }.id
        val progressed = (engine.select(first, targetId, Side.PLAYER) as SelectionOutcome.FirstSelected).state
        val restarted = engine.newGame()
        assertTrue(restarted.selectedCellIds.isEmpty())
        assertEquals(18, restarted.pairsRemaining)
        assertEquals(1, progressed.selectedCellIds.size)
    }

    @Test
    fun `full simulated games always complete with 18 pairs`() {
        repeat(200) { seed ->
            val random = Random(seed)
            val gameEngine = SymbolMatchGameEngine(random)
            val cpu = SymbolMatchCpuStrategy(random)
            var state = gameEngine.newGame()
            var guard = 0
            while (state.phase != GamePhase.GAME_OVER && guard++ < 500) {
                val difficulty = if (state.activeSide == Side.PLAYER) Difficulty.EASY else Difficulty.entries[seed % 3]
                val move = cpu.chooseMove(state.cells, state.targetSymbol, difficulty)!!
                val first = gameEngine.select(state, move.firstId, state.activeSide) as SelectionOutcome.FirstSelected
                val pair = gameEngine.select(first.state, move.secondId, state.activeSide) as SelectionOutcome.PairSelected
                state = gameEngine.resolvePair(pair.state)!!.state
            }
            assertEquals(GamePhase.GAME_OVER, state.phase)
            assertEquals(0, state.pairsRemaining)
            assertEquals(18, state.playerPairs + state.cpuPairs)
            assertTrue(state.cells.all { it.state.isCaptured })
            assertEquals(state.cells.count { it.state == CellState.PLAYER_CAPTURED }, state.playerPairs * 2)
            assertNotNull(gameEngine.result(state))
        }
    }
}
