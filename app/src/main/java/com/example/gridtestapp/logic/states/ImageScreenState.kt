package com.example.gridtestapp.logic.states

data class ImageScreenState internal constructor (
    val imageLoaded: Boolean,
    val originalUrlStates: Map<String, LoadState>,
    val index: Int,
) {

    constructor(initialIndex: Int): this(
        imageLoaded = false,
        originalUrlStates = hashMapOf(),
        index = initialIndex,
    )
}