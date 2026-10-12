package com.example.symbolmatchboard.data

import com.example.symbolmatchboard.game.model.GameOutcome
import com.example.symbolmatchboard.game.model.GameResult
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsTest {

    private fun result(player: Int, cpu: Int, score: Int = player * 12) = GameResult(
        outcome = when {
            player > cpu -> GameOutcome.PLAYER_WIN
            cpu > player -> GameOutcome.CPU_WIN
            else -> GameOutcome.DRAW
        },
        playerPairs = player,
        cpuPairs = cpu,
        playerScore = score,
        cpuScore = cpu * 12,
        totalPairs = 18,
        isPerfect = player == 18
    )

    @Test
    fun `win increments streak, draw keeps it, loss resets it`() {
        var stats = Statistics()
        stats = stats.record(result(10, 8))
        stats = stats.record(result(12, 6))
        assertEquals(2, stats.currentStreak)
        stats = stats.record(result(9, 9))
        assertEquals(2, stats.currentStreak)
        stats = stats.record(result(5, 13))
        assertEquals(0, stats.currentStreak)
        assertEquals(2, stats.bestStreak)
        assertEquals(4, stats.gamesPlayed)
        assertEquals(2, stats.wins)
        assertEquals(1, stats.losses)
        assertEquals(1, stats.draws)
    }

    @Test
    fun `best pairs and best score keep the maximum`() {
        var stats = Statistics()
        stats = stats.record(result(12, 6, score = 150))
        stats = stats.record(result(10, 8, score = 160))
        stats = stats.record(result(4, 14, score = 50))
        assertEquals(12, stats.bestPairs)
        assertEquals(160, stats.bestScore)
    }

    @Test
    fun `repository persists statistics and resets`() {
        val store = InMemoryStore()
        val repository = StatisticsRepository(store)
        repository.record(result(18, 0, score = 230))
        repository.record(result(10, 8))

        val reloaded = StatisticsRepository(store).statistics.value
        assertEquals(2, reloaded.gamesPlayed)
        assertEquals(2, reloaded.wins)
        assertEquals(18, reloaded.bestPairs)
        assertEquals(230, reloaded.bestScore)
        assertEquals(2, reloaded.currentStreak)
        assertEquals(2, reloaded.bestStreak)

        repository.reset()
        assertEquals(Statistics(), repository.statistics.value)
        assertEquals(Statistics(), StatisticsRepository(store).statistics.value)
    }
}
