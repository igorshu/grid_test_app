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
)