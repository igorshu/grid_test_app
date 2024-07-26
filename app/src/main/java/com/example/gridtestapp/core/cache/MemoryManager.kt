package com.example.gridtestapp.core.cache

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

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

    /* --- Notification --- */

    fun notificationBitmap(url: String, width: Int): Bitmap? {
        return original[url]?.let {
            if (width > it.width) {
                return it.asAndroidBitmap()
            } else {
                val height = width * it.height / it.width
                return Bitmap.createScaledBitmap(it.asAndroidBitmap(), width, height, false)
            }
        }
    }
}