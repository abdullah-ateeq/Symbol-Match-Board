package com.example.symbolmatchboard.data

import com.example.symbolmatchboard.game.model.Difficulty
import com.example.symbolmatchboard.util.GameTimings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {

    @Test
    fun `defaults match the spec`() {
        val settings = SettingsRepository(InMemoryStore()).settings.value
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.dynamicColor)
        assertEquals(Difficulty.NORMAL, settings.difficulty)
        assertTrue(settings.sound && settings.haptics && settings.animations)
        assertTrue(settings.confirmRestart && settings.showCoordinates && settings.showTargetHighlights)
    }

    @Test
    fun `settings persist across repository instances`() {
        val store = InMemoryStore()
        val changed = AppSettings(
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            difficulty = Difficulty.HARD,
            sound = false,
            haptics = false,
            animations = false,
            confirmRestart = false,
            showCoordinates = false,
            showTargetHighlights = false
        )
        SettingsRepository(store).update { changed }
        assertEquals(changed, SettingsRepository(store).settings.value)
    }

    @Test
    fun `reset settings restores defaults`() {
        val store = InMemoryStore()
        val repository = SettingsRepository(store)
        repository.update { it.copy(difficulty = Difficulty.EASY, themeMode = ThemeMode.LIGHT) }
        repository.reset()
        assertEquals(AppSettings(), repository.settings.value)
        assertEquals(AppSettings(), SettingsRepository(store).settings.value)
    }

    @Test
    fun `corrupt stored enum falls back to default`() {
        val store = InMemoryStore()
        store.edit { putString("settings_difficulty", "IMPOSSIBLE") }
        assertEquals(Difficulty.NORMAL, SettingsRepository(store).settings.value.difficulty)
    }

    @Test
    fun `animations disabled removes purely visual delays`() {
        val off = GameTimings.forAnimations(false)
        assertEquals(0L, off.matchRevealMs)
        assertEquals(0L, off.lastMatchHighlightMs)
        assertEquals(0L, off.mismatchMs)
        val on = GameTimings.forAnimations(true)
        assertTrue(on.matchRevealMs + on.lastMatchHighlightMs in 500L..700L)
        assertTrue(on.mismatchMs in 400L..600L)
        listOf(on, off).forEach {
            assertEquals(300L, it.cpuThinkMinMs)
            assertEquals(700L, it.cpuThinkMaxMs)
        }
    }
}
