package com.example.gridtestapp.core.cache

import org.koin.dsl.module

typealias OnLoading = (url: String) -> Unit
typealias OnLoaded = (url: String) -> Unit

class ImageLoader {

    suspend fun loadImage(url: String, onLoading: OnLoading?, onLoaded: OnLoaded?) {
        if (CacheManager.isNotCached(url)) {
            onLoading?.invoke(url)
            if (CacheManager.loadImage(url)) {
                onLoaded?.invoke(url)
            }
        } else {
            onLoaded?.invoke(url)
        }
    }

    companion object {
        val module = module {
            single { ImageLoader() }
        }
    }

}