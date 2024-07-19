package com.example.gridtestapp.logic.states

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class ImageState(
    val url: String,
    val imageError: ImageError?,
    val previewState: LoadState,
    val previewBitmap: ImageBitmap?,
) {

    override fun toString(): String {
        return "ImageState(previewState=$previewState, ${if (imageError != null) "imageError=$imageError," else ""}url='$url')"
    }
}