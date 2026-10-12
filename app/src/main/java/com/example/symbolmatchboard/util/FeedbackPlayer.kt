package com.example.symbolmatchboard.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** Semantic game feedback; mapped to tones and haptics by [FeedbackPlayer]. */
enum class FeedbackCue {
    FIRST_SELECTION,
    SECOND_SELECTION,
    MATCH,
    MISMATCH,
    CPU_TURN,
    CPU_CAPTURE,
    VICTORY,
    DEFEAT
}

/**
 * Lightweight, asset-free sound and haptics. Sounds are synthesized system tones so the app
 * stays fully offline and needs no audio files.
 */
class FeedbackPlayer {
    private val toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 45)
    } catch (_: RuntimeException) {
        null
    }

    fun play(cue: FeedbackCue, view: View, sound: Boolean, haptics: Boolean) {
        if (sound) playTone(cue)
        if (haptics) performHaptic(cue, view)
    }

    private fun playTone(cue: FeedbackCue) {
        val generator = toneGenerator ?: return
        val (tone, duration) = when (cue) {
            FeedbackCue.FIRST_SELECTION, FeedbackCue.SECOND_SELECTION -> ToneGenerator.TONE_PROP_BEEP to 40
            FeedbackCue.MATCH -> ToneGenerator.TONE_PROP_ACK to 120
            FeedbackCue.MISMATCH -> ToneGenerator.TONE_PROP_NACK to 140
            FeedbackCue.CPU_TURN -> ToneGenerator.TONE_CDMA_PIP to 60
            FeedbackCue.CPU_CAPTURE -> ToneGenerator.TONE_PROP_BEEP2 to 90
            FeedbackCue.VICTORY -> ToneGenerator.TONE_CDMA_CONFIRM to 300
            FeedbackCue.DEFEAT -> ToneGenerator.TONE_CDMA_ABBR_INTERCEPT to 250
        }
        try {
            generator.startTone(tone, duration)
        } catch (_: RuntimeException) {
            // Audio is optional; ignore devices that refuse tone playback.
        }
    }

    private fun performHaptic(cue: FeedbackCue, view: View) {
        val light = HapticFeedbackConstants.KEYBOARD_TAP
        val medium = HapticFeedbackConstants.CONTEXT_CLICK
        val constant = when (cue) {
            FeedbackCue.FIRST_SELECTION, FeedbackCue.SECOND_SELECTION, FeedbackCue.MISMATCH -> light
            FeedbackCue.MATCH -> medium
            FeedbackCue.VICTORY ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else medium
            FeedbackCue.DEFEAT ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
                else HapticFeedbackConstants.LONG_PRESS
            FeedbackCue.CPU_TURN, FeedbackCue.CPU_CAPTURE -> return
        }
        view.performHapticFeedback(constant)
    }

    fun release() {
        toneGenerator?.release()
    }
}
