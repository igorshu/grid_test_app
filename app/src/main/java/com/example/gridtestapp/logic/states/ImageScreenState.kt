package com.example.gridtestapp.logic.states

data class ImageScreenState internal constructor (
    val imageLoaded: Boolean,
    val originalUrlStates: Map<String, LoadState>,
    val index: Int,
) {
    companion object {
        fun init(initialIndex: Int) = ImageScreenState(
            imageLoaded = false,
            originalUrlStates = hashMapOf(),
            index = initialIndex,
        )
    }
}