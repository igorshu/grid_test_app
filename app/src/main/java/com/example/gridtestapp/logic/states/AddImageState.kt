package com.example.gridtestapp.logic.states

data class AddImageState internal constructor(
    val loadState: LoadState,
    val imageError: ImageError?,
) {
    constructor() : this(LoadState.IDLE, imageError = null)
}