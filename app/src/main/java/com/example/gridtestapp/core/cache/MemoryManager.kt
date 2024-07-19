package com.example.gridtestapp.core.cache

import androidx.compose.ui.graphics.ImageBitmap

/*
*
*
*  Здесь храним bitmap-ы в памяти
*
*/

object MemoryManager {

    private var original: MutableMap<String, ImageBitmap?> = mutableMapOf()

    /* --- Original --- */

    fun addOriginalBitmap(url: String, bitmap: ImageBitmap) {
        original[url] = bitmap.apply { prepareToDraw() }
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
        original.remove(url)
    }

    fun clearAll() {
        original.clear()
    }
}