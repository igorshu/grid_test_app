package com.example.gridtestapp.logic.states

import androidx.compose.runtime.Immutable
import arrow.core.None
import arrow.core.Option

enum class Screen {
    MAIN, IMAGE, ADD_IMAGE
}

enum class Theme {
    BY_DEFAULT, LIGHT, DARK;

    fun isDark(systemTheme: Boolean): Boolean {
        return this == DARK || systemTheme && this == BY_DEFAULT
    }
}

@Immutable
data class AppState internal constructor (

    val theme: Theme,
    val loading: Boolean,
    val imageStatesHashcode: Int?,
    val showImageFailDialog: Option<String>,
    val inetAvailable: Boolean,
    val showBack: Boolean,
    val showTopBar: Boolean,
    val showSystemBars: Boolean,
    val title: String,
    val currentScreen: Screen,
    val currentImage: ImagePair?,
    val selectedImage: ImageSelected?,
    val sharedAnimation: Boolean,
) {

    constructor(title: String) : this(
        theme = Theme.BY_DEFAULT,
        loading = true,
        imageStatesHashcode = null,
        showImageFailDialog = None,
        inetAvailable = true,
        showBack = false,
        showTopBar = true,
        showSystemBars = true,
        title = title,
        currentScreen = Screen.MAIN,
        currentImage = null,
        selectedImage = null,
        sharedAnimation = true,
    )

    fun clear() = copy(
        loading = true,
        showImageFailDialog = None,
    )

    @Immutable
    data class ImagePair(
        val url: String,
        val index: Int,
    )

    @Immutable
    data class ImageSelected(
        val url: String,
        val index: Int,
        val consumed: Boolean = false
    )
}