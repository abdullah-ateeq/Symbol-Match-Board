package com.example.symbolmatchboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.symbolmatchboard.game.model.SymbolType

/**
 * Subtle, muted accents per symbol. Shape alone is enough to tell symbols apart;
 * color is only a secondary cue.
 */
class SymbolColors(private val colors: Map<SymbolType, Color>) {
    operator fun get(symbol: SymbolType): Color = colors.getValue(symbol)
}

val LightSymbolColors = SymbolColors(
    mapOf(
        SymbolType.CIRCLE to Color(0xFF3D6E9E),
        SymbolType.TRIANGLE to Color(0xFFA9652A),
        SymbolType.SQUARE to Color(0xFF4F5E6B),
        SymbolType.STAR to Color(0xFF9C7A12),
        SymbolType.DIAMOND to Color(0xFF7A4E9A),
        SymbolType.HEXAGON to Color(0xFF2F7D63),
        SymbolType.MOON to Color(0xFF4C5BA6),
        SymbolType.BOLT to Color(0xFFB04E3B),
        SymbolType.CROSS to Color(0xFF8A4560)
    )
)

val DarkSymbolColors = SymbolColors(
    mapOf(
        SymbolType.CIRCLE to Color(0xFF8DB6DD),
        SymbolType.TRIANGLE to Color(0xFFE0A877),
        SymbolType.SQUARE to Color(0xFFB4C0CA),
        SymbolType.STAR to Color(0xFFE3C66A),
        SymbolType.DIAMOND to Color(0xFFC3A1DB),
        SymbolType.HEXAGON to Color(0xFF85CBB0),
        SymbolType.MOON to Color(0xFFA5B0E6),
        SymbolType.BOLT to Color(0xFFE79A89),
        SymbolType.CROSS to Color(0xFFD99AB3)
    )
)

val LocalSymbolColors = staticCompositionLocalOf { LightSymbolColors }

object BoardTheme {
    val symbolColors: SymbolColors
        @Composable @ReadOnlyComposable get() = LocalSymbolColors.current
}
