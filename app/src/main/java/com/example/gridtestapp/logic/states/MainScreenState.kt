package com.example.gridtestapp.logic.states

data class MainScreenState(
    val widthConsumed: Boolean,
) {
    companion object {
        fun init() = MainScreenState(
            widthConsumed = false,
        )
    }
}