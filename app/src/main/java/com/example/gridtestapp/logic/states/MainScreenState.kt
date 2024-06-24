package com.example.gridtestapp.logic.states

data class MainScreenState(
    val urls: List<String>,
    val urlStates: Map<String, LoadState>,
    val widthConsumed: Boolean,
    val inetAvailable: Boolean,
    val screenRange: IntRange,
    val preloadRange: IntRange,
)