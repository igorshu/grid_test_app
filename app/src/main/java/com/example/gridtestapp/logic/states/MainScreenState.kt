package com.example.gridtestapp.logic.states

import arrow.core.None
import arrow.core.Option

data class MainScreenState(
    val urls: List<String>,
    val urlStates: Map<String, LoadState>,
    val widthConsumed: Boolean,
    val screenRange: IntRange,
    val preloadRange: IntRange,
    val showImageFailDialog: Option<String>,
    val imageErrors: Map<String, ImageError>
) {
    class ImageError(
        val errorMessage: String,
        val canBeLoad: Boolean,
    )

    companion object {
        fun init() = MainScreenState(
            urls = listOf(),
            urlStates = hashMapOf(),
            widthConsumed = false,
            screenRange = IntRange(0, 0),
            preloadRange = IntRange(0, 0),
            showImageFailDialog = None,
            imageErrors = hashMapOf(),
        )
    }
}