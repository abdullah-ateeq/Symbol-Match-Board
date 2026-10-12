package com.example.symbolmatchboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.symbolmatchboard.SymbolMatchApp
import com.example.symbolmatchboard.data.AppSettings
import com.example.symbolmatchboard.data.Statistics
import kotlinx.coroutines.flow.StateFlow

/** Settings and statistics shared by Home, Settings and the theme. */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsRepository = (app as SymbolMatchApp).settingsRepository
    private val statisticsRepository = (app as SymbolMatchApp).statisticsRepository

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val statistics: StateFlow<Statistics> = statisticsRepository.statistics

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsRepository.update(transform)

    fun resetSettings() = settingsRepository.reset()

    fun resetStatistics() = statisticsRepository.reset()
}
