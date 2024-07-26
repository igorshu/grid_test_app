package com.example.gridtestapp.ui.other

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

class MultiBrushPainter(
    val brushes: List<Brush>,
) : Painter() {

    private var alpha: Float = 1.0f
    private var colorFilter: ColorFilter? = null

    override val intrinsicSize: Size
        get() = brushes[0].intrinsicSize

    override fun DrawScope.onDraw() {
        brushes.forEach { brush ->
            drawRect(brush = brush, alpha = alpha, colorFilter = colorFilter)
        }
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MultiBrushPainter) return false

        if (brushes != other.brushes) return false

        return true
    }

    override fun hashCode(): Int {
        return brushes.hashCode()
    }

    override fun toString(): String {
        return "MultiBrushPainter (brushes = ${brushes.joinToString(", ") { it.toString() }})"
    }
}
