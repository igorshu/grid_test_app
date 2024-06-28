package com.example.gridtestapp.logic.states

enum class Screen {
    MAIN, IMAGE
}

data class AppState internal constructor (

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