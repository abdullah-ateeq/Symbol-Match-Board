package com.example.symbolmatchboard.data

import com.example.symbolmatchboard.game.model.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val animations: Boolean = true,
    val confirmRestart: Boolean = true,
    val showCoordinates: Boolean = true,
    val showTargetHighlights: Boolean = true
)

class SettingsRepository(private val store: KeyValueStore) {

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        save(updated)
        _settings.value = updated
    }

    fun reset() {
        store.clear(ALL_KEYS)
        _settings.value = AppSettings()
    }

    private fun load(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = enumOrDefault(store.getString(KEY_THEME, defaults.themeMode.name), defaults.themeMode),
            dynamicColor = store.getBoolean(KEY_DYNAMIC, defaults.dynamicColor),
            difficulty = enumOrDefault(store.getString(KEY_DIFFICULTY, defaults.difficulty.name), defaults.difficulty),
            sound = store.getBoolean(KEY_SOUND, defaults.sound),
            haptics = store.getBoolean(KEY_HAPTICS, defaults.haptics),
            animations = store.getBoolean(KEY_ANIMATIONS, defaults.animations),
            confirmRestart = store.getBoolean(KEY_CONFIRM_RESTART, defaults.confirmRestart),
            showCoordinates = store.getBoolean(KEY_COORDINATES, defaults.showCoordinates),
            showTargetHighlights = store.getBoolean(KEY_TARGET_HIGHLIGHTS, defaults.showTargetHighlights)
        )
    }

    private fun save(settings: AppSettings) {
        store.edit {
            putString(KEY_THEME, settings.themeMode.name)
            putBoolean(KEY_DYNAMIC, settings.dynamicColor)
            putString(KEY_DIFFICULTY, settings.difficulty.name)
            putBoolean(KEY_SOUND, settings.sound)
            putBoolean(KEY_HAPTICS, settings.haptics)
            putBoolean(KEY_ANIMATIONS, settings.animations)
            putBoolean(KEY_CONFIRM_RESTART, settings.confirmRestart)
            putBoolean(KEY_COORDINATES, settings.showCoordinates)
            putBoolean(KEY_TARGET_HIGHLIGHTS, settings.showTargetHighlights)
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val KEY_THEME = "settings_theme"
        const val KEY_DYNAMIC = "settings_dynamic_color"
        const val KEY_DIFFICULTY = "settings_difficulty"
        const val KEY_SOUND = "settings_sound"
        const val KEY_HAPTICS = "settings_haptics"
        const val KEY_ANIMATIONS = "settings_animations"
        const val KEY_CONFIRM_RESTART = "settings_confirm_restart"
        const val KEY_COORDINATES = "settings_coordinates"
        const val KEY_TARGET_HIGHLIGHTS = "settings_target_highlights"
        val ALL_KEYS = listOf(
            KEY_THEME, KEY_DYNAMIC, KEY_DIFFICULTY, KEY_SOUND, KEY_HAPTICS,
            KEY_ANIMATIONS, KEY_CONFIRM_RESTART, KEY_COORDINATES, KEY_TARGET_HIGHLIGHTS
        )
    }
}
