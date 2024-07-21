package com.example.gridtestapp.logic.states

import androidx.compose.ui.graphics.ImageBitmap


data class ImageState(
    val url: String,
    var imageError: ImageError?,
    var previewState: LoadState,
    var previewBitmap: ImageBitmap?,
) {

    override fun toString(): String {
        return "ImageState(previewState=$previewState, ${if (imageError != null) "imageError=$imageError," else ""}url='$url')"
    }
}