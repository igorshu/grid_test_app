package com.example.gridtestapp.core.cache

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ImageColors(
    val bottomLeft: Color,
    val topLeft: Color,
    val topRight: Color,
    val bottomRight: Color,
    val center: Color,
) {

    companion object {
        fun from(colors: Array<Color>): ImageColors {
            return ImageColors(
                topLeft = colors[0],
                topRight = colors[1],
                bottomRight = colors[2],
                bottomLeft = colors[3],
                center = colors[4],
            )
        }
    }
}