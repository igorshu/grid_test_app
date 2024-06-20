package com.example.gridtestapp.logic.states

import androidx.compose.ui.graphics.ImageBitmap

data class MainScreenState(
    val urlStates: Map<String, LoadState>,
    val previewBitmaps: Map<String, ImageBitmap>,
    val widthConsumed: Boolean,
)