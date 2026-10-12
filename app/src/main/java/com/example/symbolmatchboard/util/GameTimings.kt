package com.example.symbolmatchboard.util

/** All gameplay pacing in one place. With animations off, purely visual waits drop to zero. */
data class GameTimings(
    val cpuThinkMinMs: Long,
    val cpuThinkMaxMs: Long,
    val cpuSecondPickMs: Long,
    val matchRevealMs: Long,
    val lastMatchHighlightMs: Long,
    val mismatchMs: Long,
    val gameOverDelayMs: Long
) {
    companion object {
        fun forAnimations(enabled: Boolean): GameTimings = if (enabled) {
            GameTimings(
                cpuThinkMinMs = 300,
                cpuThinkMaxMs = 700,
                cpuSecondPickMs = 250,
                matchRevealMs = 320,
                lastMatchHighlightMs = 320,
                mismatchMs = 500,
                gameOverDelayMs = 700
            )
        } else {
            GameTimings(
                cpuThinkMinMs = 300,
                cpuThinkMaxMs = 700,
                cpuSecondPickMs = 150,
                matchRevealMs = 0,
                lastMatchHighlightMs = 0,
                mismatchMs = 0,
                gameOverDelayMs = 300
            )
        }
    }
}
