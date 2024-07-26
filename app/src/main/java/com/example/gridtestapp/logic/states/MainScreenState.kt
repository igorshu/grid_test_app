package com.example.gridtestapp.logic.states

data class MainScreenState(
    val widthConsumed: Boolean,
) {
    constructor(): this(
        widthConsumed = false,
    )
}