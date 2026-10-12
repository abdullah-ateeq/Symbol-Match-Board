package com.example.symbolmatchboard.data

import com.example.symbolmatchboard.game.model.GameOutcome
import com.example.symbolmatchboard.game.model.GameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Statistics(
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val bestPairs: Int = 0,
    val bestScore: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0
) {
    /** Win: streak + 1. Loss: streak resets. Draw: streak unchanged. */
    fun record(result: GameResult): Statistics {
        val streak = when (result.outcome) {
            GameOutcome.PLAYER_WIN -> currentStreak + 1
            GameOutcome.CPU_WIN -> 0
            GameOutcome.DRAW -> currentStreak
        }
        return copy(
            gamesPlayed = gamesPlayed + 1,
            wins = wins + if (result.outcome == GameOutcome.PLAYER_WIN) 1 else 0,
            losses = losses + if (result.outcome == GameOutcome.CPU_WIN) 1 else 0,
            draws = draws + if (result.outcome == GameOutcome.DRAW) 1 else 0,
            bestPairs = maxOf(bestPairs, result.playerPairs),
            bestScore = maxOf(bestScore, result.playerScore),
            currentStreak = streak,
            bestStreak = maxOf(bestStreak, streak)
        )
    }
}

class StatisticsRepository(private val store: KeyValueStore) {

    private val _statistics = MutableStateFlow(load())
    val statistics: StateFlow<Statistics> = _statistics.asStateFlow()

    fun record(result: GameResult): Statistics {
        val updated = _statistics.value.record(result)
        save(updated)
        _statistics.value = updated
        return updated
    }

    fun reset() {
        store.clear(ALL_KEYS)
        _statistics.value = Statistics()
    }

    private fun load() = Statistics(
        gamesPlayed = store.getInt(KEY_GAMES, 0),
        wins = store.getInt(KEY_WINS, 0),
        losses = store.getInt(KEY_LOSSES, 0),
        draws = store.getInt(KEY_DRAWS, 0),
        bestPairs = store.getInt(KEY_BEST_PAIRS, 0),
        bestScore = store.getInt(KEY_BEST_SCORE, 0),
        currentStreak = store.getInt(KEY_STREAK, 0),
        bestStreak = store.getInt(KEY_BEST_STREAK, 0)
    )

    private fun save(stats: Statistics) {
        store.edit {
            putInt(KEY_GAMES, stats.gamesPlayed)
            putInt(KEY_WINS, stats.wins)
            putInt(KEY_LOSSES, stats.losses)
            putInt(KEY_DRAWS, stats.draws)
            putInt(KEY_BEST_PAIRS, stats.bestPairs)
            putInt(KEY_BEST_SCORE, stats.bestScore)
            putInt(KEY_STREAK, stats.currentStreak)
            putInt(KEY_BEST_STREAK, stats.bestStreak)
        }
    }

    private companion object {
        const val KEY_GAMES = "stats_games"
        const val KEY_WINS = "stats_wins"
        const val KEY_LOSSES = "stats_losses"
        const val KEY_DRAWS = "stats_draws"
        const val KEY_BEST_PAIRS = "stats_best_pairs"
        const val KEY_BEST_SCORE = "stats_best_score"
        const val KEY_STREAK = "stats_current_streak"
        const val KEY_BEST_STREAK = "stats_best_streak"
        val ALL_KEYS = listOf(
            KEY_GAMES, KEY_WINS, KEY_LOSSES, KEY_DRAWS,
            KEY_BEST_PAIRS, KEY_BEST_SCORE, KEY_STREAK, KEY_BEST_STREAK
        )
    }
}
