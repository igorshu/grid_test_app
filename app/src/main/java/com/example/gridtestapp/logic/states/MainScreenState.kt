package com.example.gridtestapp.logic.states

import androidx.compose.ui.graphics.ImageBitmap

data class MainScreenState(
    val urls: List<String>,
    val urlStates: Map<String, LoadState>,
    val widthConsumed: Boolean,
)