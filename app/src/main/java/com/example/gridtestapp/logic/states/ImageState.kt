package com.example.gridtestapp.logic.states

import androidx.compose.ui.graphics.ImageBitmap
import com.example.gridtestapp.core.cache.ImageColors


data class ImageState(
    val url: String,
    var imageError: ImageError?,
    var previewState: LoadState,
    var previewBitmap: ImageBitmap?,
    var imageColors: ImageColors?,
) {

    override fun toString(): String {
        return "ImageState(previewState=$previewState, ${if (imageError != null) "imageError=$imageError," else ""}url='$url')"
    }
}