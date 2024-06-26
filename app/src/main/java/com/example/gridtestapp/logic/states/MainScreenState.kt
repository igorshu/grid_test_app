package com.example.gridtestapp.logic.states

import arrow.core.Option

data class MainScreenState(
    val urls: List<String>,
    val urlStates: Map<String, LoadState>,
    val widthConsumed: Boolean,
    val inetAvailable: Boolean,
    val screenRange: IntRange,
    val preloadRange: IntRange,
    val showImageFailDialog: Option<String>,
    val imageErrors: Map<String, ImageError>


) {
    class ImageError(
        val errorMessage: String,
        val canBeLoad: Boolean,
    )
}