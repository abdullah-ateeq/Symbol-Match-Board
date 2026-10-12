package com.example.symbolmatchboard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.symbolmatchboard.game.model.SymbolType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Original, asset-free vector symbol. Each one is identifiable by silhouette alone. */
@Composable
fun SymbolIcon(symbol: SymbolType, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) { drawSymbol(symbol, color) }
}

/** Draws [symbol] centered in the current draw area. */
fun DrawScope.drawSymbol(symbol: SymbolType, color: Color) {
    val extent = min(size.width, size.height)
    drawSymbol(symbol, color, Offset((size.width - extent) / 2f, (size.height - extent) / 2f), extent)
}

/** Draws [symbol] inside the square starting at [topLeft] with side [extent]. */
fun DrawScope.drawSymbol(symbol: SymbolType, color: Color, topLeft: Offset, extent: Float) {
    val s = extent
    val ox = topLeft.x
    val oy = topLeft.y
    fun p(x: Float, y: Float) = Offset(ox + x * s, oy + y * s)
    val soften = Stroke(width = s * 0.07f, join = StrokeJoin.Round, cap = StrokeCap.Round)

    fun polygon(points: List<Offset>) {
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(path, color)
        drawPath(path, color, style = soften)
    }

    when (symbol) {
        SymbolType.CIRCLE -> drawCircle(color, radius = s * 0.34f, center = p(0.5f, 0.5f))

        SymbolType.TRIANGLE -> polygon(listOf(p(0.5f, 0.16f), p(0.85f, 0.79f), p(0.15f, 0.79f)))

        SymbolType.SQUARE -> drawRoundRect(
            color,
            topLeft = p(0.21f, 0.21f),
            size = Size(s * 0.58f, s * 0.58f),
            cornerRadius = CornerRadius(s * 0.07f)
        )

        SymbolType.STAR -> {
            val center = p(0.5f, 0.53f)
            val points = (0 until 10).map { i ->
                val radius = if (i % 2 == 0) s * 0.40f else s * 0.17f
                val angle = -PI / 2 + i * PI / 5
                Offset(center.x + (radius * cos(angle)).toFloat(), center.y + (radius * sin(angle)).toFloat())
            }
            polygon(points)
        }

        SymbolType.DIAMOND -> polygon(listOf(p(0.5f, 0.12f), p(0.82f, 0.5f), p(0.5f, 0.88f), p(0.18f, 0.5f)))

        SymbolType.HEXAGON -> {
            val center = p(0.5f, 0.5f)
            val points = (0 until 6).map { i ->
                val angle = PI / 6 + i * PI / 3
                Offset(center.x + (s * 0.37f * cos(angle)).toFloat(), center.y + (s * 0.37f * sin(angle)).toFloat())
            }
            polygon(points)
        }

        SymbolType.MOON -> {
            val outer = Path().apply { addOval(Rect(p(0.5f, 0.5f), s * 0.36f)) }
            val cut = Path().apply { addOval(Rect(p(0.68f, 0.38f), s * 0.29f)) }
            val crescent = Path().apply { op(outer, cut, PathOperation.Difference) }
            drawPath(crescent, color)
        }

        SymbolType.BOLT -> polygon(
            listOf(
                p(0.60f, 0.10f), p(0.25f, 0.56f), p(0.47f, 0.56f),
                p(0.40f, 0.90f), p(0.76f, 0.43f), p(0.54f, 0.43f), p(0.63f, 0.10f)
            )
        )

        SymbolType.CROSS -> {
            val arm = s * 0.22f
            val length = s * 0.66f
            val radius = CornerRadius(s * 0.05f)
            drawRoundRect(color, topLeft = p(0.5f, 0.17f) - Offset(arm / 2, 0f), size = Size(arm, length), cornerRadius = radius)
            drawRoundRect(color, topLeft = p(0.17f, 0.5f) - Offset(0f, arm / 2), size = Size(length, arm), cornerRadius = radius)
        }
    }
}

/** Player ownership: check mark inside a ring. */
fun DrawScope.drawPlayerMarker(color: Color) {
    val s = min(size.width, size.height)
    val c = center
    val stroke = s * 0.075f
    drawCircle(color, radius = s * 0.30f, center = c, style = Stroke(width = stroke))
    val check = Path().apply {
        moveTo(c.x - s * 0.13f, c.y + s * 0.01f)
        lineTo(c.x - s * 0.03f, c.y + s * 0.11f)
        lineTo(c.x + s * 0.15f, c.y - s * 0.10f)
    }
    drawPath(check, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** CPU ownership: a small "chip" — rounded square with a solid core and pins. */
fun DrawScope.drawCpuMarker(color: Color) {
    val s = min(size.width, size.height)
    val c = center
    val stroke = s * 0.065f
    val half = s * 0.22f
    drawRoundRect(
        color,
        topLeft = Offset(c.x - half, c.y - half),
        size = Size(half * 2, half * 2),
        cornerRadius = CornerRadius(s * 0.06f),
        style = Stroke(width = stroke)
    )
    val core = s * 0.08f
    drawRect(color, topLeft = Offset(c.x - core, c.y - core), size = Size(core * 2, core * 2))
    val pin = s * 0.07f
    listOf(-0.1f, 0.1f).forEach { o ->
        val d = o * s
        drawLine(color, Offset(c.x + d, c.y - half), Offset(c.x + d, c.y - half - pin), stroke, StrokeCap.Round)
        drawLine(color, Offset(c.x + d, c.y + half), Offset(c.x + d, c.y + half + pin), stroke, StrokeCap.Round)
        drawLine(color, Offset(c.x - half, c.y + d), Offset(c.x - half - pin, c.y + d), stroke, StrokeCap.Round)
        drawLine(color, Offset(c.x + half, c.y + d), Offset(c.x + half + pin, c.y + d), stroke, StrokeCap.Round)
    }
}
