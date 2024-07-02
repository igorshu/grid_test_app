package com.example.gridtestapp.logic.states

data class MainScreenState(
    val widthConsumed: Boolean,
) {
    class ImageError(
        val errorMessage: String,
        val canBeLoad: Boolean,
    )

    companion object {
        fun init() = MainScreenState(
            widthConsumed = false,
        )
    }
}