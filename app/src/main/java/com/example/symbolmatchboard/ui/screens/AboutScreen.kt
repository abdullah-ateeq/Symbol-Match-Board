package com.example.symbolmatchboard.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.symbolmatchboard.ui.components.SectionCard

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember { appVersion(context) }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark(Modifier.size(72.dp))
            Text(
                "SYMBOL MATCH BOARD",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Text("Version $version", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "An offline competitive symbol-matching game where visible symbols are matched and captured.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            SectionCard(title = "HOW IT WORKS") {
                Text("Find matching symbols scattered across the board and capture pairs before the CPU.")
            }
            SectionCard(title = "BOARD") {
                Text("6 × 6 board with 18 matching pairs. Nine symbols, each appearing four times — all visible from the start.")
            }
            SectionCard(title = "PRIVACY") {
                Text("No account. No internet connection. No personal data collection.")
            }
        }
    }
}

private fun appVersion(context: Context): String = try {
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    info.versionName ?: "1.0"
} catch (_: Exception) {
    "1.0"
}
