package com.example.gridtestapp.logic.states

import arrow.core.None
import arrow.core.Option

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