package com.example.symbolmatchboard

import android.app.Application
import android.content.Context
import com.example.symbolmatchboard.data.SettingsRepository
import com.example.symbolmatchboard.data.SharedPreferencesStore
import com.example.symbolmatchboard.data.StatisticsRepository

class SymbolMatchApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var statisticsRepository: StatisticsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val store = SharedPreferencesStore(getSharedPreferences("symbol_match_board", Context.MODE_PRIVATE))
        settingsRepository = SettingsRepository(store)
        statisticsRepository = StatisticsRepository(store)
    }
}
