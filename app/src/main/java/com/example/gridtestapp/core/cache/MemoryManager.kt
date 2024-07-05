package com.example.gridtestapp.core.cache

import androidx.compose.ui.graphics.ImageBitmap

/*
*
*
*  Здесь храним bitmap-ы в памяти
*
*/

object MemoryManager {

    private var previews: MutableMap<String, ImageBitmap?> = mutableMapOf()
    private var original: MutableMap<String, ImageBitmap?> = mutableMapOf()

    fun addPreviewBitmap(url: String, bitmap: ImageBitmap) {
        previews[url] = bitmap
    }

    fun getPreviewBitmap(url: String): ImageBitmap? {
        return previews[url]
    }

    fun removePreviewBitmap(url: String) {
        previews.remove(url)
    }

    fun previewExists(url: String) = getPreviewBitmap(url) != null

    fun clearPreviews() = previews.clear()

    /* --- Original --- */

    fun addOriginalBitmap(url: String, bitmap: ImageBitmap) {
        original[url] = bitmap
    }

    fun getOriginalBitmap(url: String): ImageBitmap? {
        return original[url]
    }

    fun removeOriginalBitmap(url: String) {
        original.remove(url)
    }

    fun originalExists(url: String) = getOriginalBitmap(url) != null


    fun clearOriginals() = original.clear()

    /* --- Common --- */

    fun removeBothImages(url: String) {
        previews.remove(url)
        original.remove(url)
    }

    fun clearAll() {
        previews.clear()
        original.clear()
    }
}