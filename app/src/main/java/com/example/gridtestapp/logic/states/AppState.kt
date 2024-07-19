package com.example.gridtestapp.logic.states

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.graphics.ImageBitmap
import arrow.core.None
import arrow.core.Option
import java.util.LinkedList

enum class Screen {
    MAIN, IMAGE, ADD_IMAGE
}

enum class Theme {
    BY_DEFAULT, LIGHT, DARK;

    fun isDark(systemTheme: Boolean): Boolean {
        return this == DARK || systemTheme && this == BY_DEFAULT
    }
}

data class AppState internal constructor (

    val theme: Theme,
    val loading: Boolean,
    val imageStates: List<ImageState>,
    val showImageFailDialog: Option<String>,
    val screenRange: IntRange,
    val preloadRange: IntRange,
    val inetAvailable: Boolean,
    val showBack: Boolean,
    val showTopBar: Boolean,
    val showSystemBars: Boolean,
    val title: String,
    val currentScreen: Screen,
    val currentImage: ImagePair?,
    val selectedImage: ImageSelected?,
    val hideImage: Boolean
) {
    companion object {
        fun init(title: String): AppState = AppState(
            theme = Theme.BY_DEFAULT,
            loading = true,
            imageStates = listOf(),
            showImageFailDialog = None,
            screenRange = IntRange(0, 0),
            preloadRange = IntRange(0, 0),
            inetAvailable = true,
            showBack = false,
            showTopBar = true,
            showSystemBars = true,
            title = title,
            currentScreen = Screen.MAIN,
            currentImage = null,
            selectedImage = null,
            hideImage = false,
        )
    }

    fun clear() = copy(
        loading = true,
        imageStates = listOf(),
        showImageFailDialog = None,
        screenRange = IntRange(0, 0),
        preloadRange = IntRange(0, 0),
    )

    class ImagePair(
        val url: String,
        val index: Int,
    )

    class ImageSelected(
        val url: String,
        val index: Int,
        val consumed: Boolean = false
    )
}