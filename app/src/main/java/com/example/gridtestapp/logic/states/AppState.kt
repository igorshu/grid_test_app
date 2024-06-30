package com.example.gridtestapp.logic.states

import arrow.core.None
import arrow.core.Option

enum class Screen {
    MAIN, IMAGE
}

data class AppState internal constructor (

    val urls: List<String>,
    val urlStates: Map<String, LoadState>,
    val showImageFailDialog: Option<String>,
    val imageErrors: Map<String, MainScreenState.ImageError>,
    val screenRange: IntRange,
    val preloadRange: IntRange,
    val inetAvailable: Boolean,
    val showBack: Boolean,
    val showTopBar: Boolean,
    val showSystemBars: Boolean,
    val title: String,
    val currentScreen: Screen,
    val shareUrl: String?

) {

    companion object {
        fun init(title: String): AppState = AppState(
            urls = listOf(),
            urlStates = hashMapOf(),
            showImageFailDialog = None,
            imageErrors = hashMapOf(),
            screenRange = IntRange(0, 0),
            preloadRange = IntRange(0, 0),
            inetAvailable = true,
            showBack = false,
            showTopBar = true,
            showSystemBars = true,
            title = title,
            currentScreen = Screen.MAIN,
            shareUrl = null
        )
    }
}