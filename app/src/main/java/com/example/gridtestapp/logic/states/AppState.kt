package com.example.gridtestapp.logic.states

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import arrow.core.None
import arrow.core.Option

enum class Screen {
    MAIN, IMAGE
}

enum class Theme {
    BY_DEFAULT, LIGHT, DARK;

    fun isDark(systemTheme: Boolean): Boolean {
        return this == DARK || systemTheme && this == BY_DEFAULT
    }
}

data class AppState internal constructor (

    val theme: Theme,
    val urls: List<String>,
    val previewUrlStates: Map<String, LoadState>,
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
    val currentImageUrl: String?,
    val deletingImage: Boolean
) {
    companion object {
        fun init(title: String): AppState = AppState(
            theme = Theme.BY_DEFAULT,
            urls = listOf(),
            previewUrlStates = hashMapOf(),
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
            currentImageUrl = null,
            deletingImage = false,
        )
    }

    fun clear() = copy(
        urls = listOf(),
        previewUrlStates = hashMapOf(),
        showImageFailDialog = None,
        imageErrors = hashMapOf(),
        screenRange = IntRange(0, 0),
        preloadRange = IntRange(0, 0),
    )

    @Composable
    fun isDark(): Boolean {
        return theme == Theme.DARK || isSystemInDarkTheme() && theme == Theme.BY_DEFAULT
    }
}