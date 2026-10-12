package com.example.symbolmatchboard.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.symbolmatchboard.data.AppSettings
import com.example.symbolmatchboard.data.ThemeMode
import com.example.symbolmatchboard.game.model.Difficulty
import com.example.symbolmatchboard.ui.components.SectionCard

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onResetStatistics: () -> Unit,
    onResetSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmResetStats by rememberSaveable { mutableStateOf(false) }
    var confirmResetSettings by rememberSaveable { mutableStateOf(false) }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("SETTINGS", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })

            SectionCard(title = "APPEARANCE") {
                Text("Theme", style = MaterialTheme.typography.bodyLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { onUpdate { it.copy(themeMode = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                        ) { Text(mode.label) }
                    }
                }
                SettingSwitch(
                    title = "Dynamic Color",
                    subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "Use colors from your wallpaper."
                    } else {
                        "Requires Android 12 or newer."
                    },
                    checked = settings.dynamicColor,
                    onCheckedChange = { value -> onUpdate { it.copy(dynamicColor = value) } }
                )
            }

            SectionCard(title = "GAMEPLAY") {
                Text("CPU Difficulty", style = MaterialTheme.typography.bodyLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Difficulty.entries.forEachIndexed { index, difficulty ->
                        SegmentedButton(
                            selected = settings.difficulty == difficulty,
                            onClick = { onUpdate { it.copy(difficulty = difficulty) } },
                            shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size)
                        ) { Text(difficulty.label) }
                    }
                }
                SettingSwitch(
                    "Show Coordinates", "Label columns A–F and rows 1–6.", settings.showCoordinates
                ) { value -> onUpdate { it.copy(showCoordinates = value) } }
                SettingSwitch(
                    "Show Target Highlights", "Outline every cell holding the target symbol.", settings.showTargetHighlights
                ) { value -> onUpdate { it.copy(showTargetHighlights = value) } }
                SettingSwitch(
                    "Confirm Restart", "Ask before restarting or leaving a match.", settings.confirmRestart
                ) { value -> onUpdate { it.copy(confirmRestart = value) } }
            }

            SectionCard(title = "FEEDBACK") {
                SettingSwitch("Sound", "Short tones for matches and turns.", settings.sound) { value ->
                    onUpdate { it.copy(sound = value) }
                }
                SettingSwitch("Haptic Feedback", "Vibration on selections and results.", settings.haptics) { value ->
                    onUpdate { it.copy(haptics = value) }
                }
                SettingSwitch("Animations", "Pulses, connecting lines and transitions.", settings.animations) { value ->
                    onUpdate { it.copy(animations = value) }
                }
            }

            SectionCard(title = "DATA") {
                OutlinedButton(onClick = { confirmResetStats = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Reset Statistics")
                }
                OutlinedButton(onClick = { confirmResetSettings = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Reset Settings")
                }
            }
        }
    }

    if (confirmResetStats) {
        ConfirmDialog(
            title = "Reset statistics?",
            text = "Games, wins, best pairs, best score and streaks will be cleared.",
            confirmLabel = "Reset",
            onConfirm = {
                confirmResetStats = false
                onResetStatistics()
            },
            onDismiss = { confirmResetStats = false }
        )
    }
    if (confirmResetSettings) {
        ConfirmDialog(
            title = "Reset settings?",
            text = "All settings return to their defaults.",
            confirmLabel = "Reset",
            onConfirm = {
                confirmResetSettings = false
                onResetSettings()
            },
            onDismiss = { confirmResetSettings = false }
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
