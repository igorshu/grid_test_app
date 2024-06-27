package com.example.gridtestapp.logic.states

enum class Screen {
    MAIN, IMAGE
}

data class AppState (

    val showBack: Boolean,
    val showTopBar: Boolean,
    val showSystemBars: Boolean,
    val title: String,
    val currentScreen: Screen,
    val shareUrl: String?

)