package com.example.gridtestapp.logic.states

import androidx.compose.ui.graphics.ImageBitmap

data class ImageScreenState internal constructor (
    val imageLoaded: Boolean,
    val originalImages: Map<String, ImageBitmap?>,
    val index: Int,
) {
    companion object {
        fun init(initialIndex: Int) = ImageScreenState(
            imageLoaded = false,
            originalImages = hashMapOf(),
            index = initialIndex,
        )
    }
}