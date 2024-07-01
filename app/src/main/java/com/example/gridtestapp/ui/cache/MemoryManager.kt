package com.example.gridtestapp.ui.cache

import androidx.compose.ui.graphics.ImageBitmap

/*
*
*
*  Здесь храним bitmap-ы в памяти
*
*/

object MemoryManager {

    private var previews: MutableMap<String, ImageBitmap> = mutableMapOf()
    private var original: MutableMap<String, ImageBitmap> = mutableMapOf()

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
}