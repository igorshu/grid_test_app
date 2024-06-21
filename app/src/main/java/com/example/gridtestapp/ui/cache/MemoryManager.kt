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

    fun addBitmap(url: String, bitmap: ImageBitmap) {
        previews[url] = bitmap
    }

    fun getBitmap(url: String): ImageBitmap? {
        return previews[url]
    }

    fun removeBitmap(url: String) {
        previews.remove(url)
    }


}