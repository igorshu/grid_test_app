package com.example.gridtestapp.logic.coroutines

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.gridtestapp.core.connection.ConnectionManager
import com.example.gridtestapp.core.cache.CacheManager
import com.example.gridtestapp.ui.exceptions.ImageLoadException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException


typealias ImageLoadFail = (url: String, errorMessage: String, canBeLoad: Boolean) -> Unit
typealias UnknownFail = (throwable: Throwable) -> Unit

fun showError(context: Context, coroutineScope: CoroutineScope, throwable: Throwable) {
    coroutineScope.launch {
        Toast.makeText(context, throwable.message, Toast.LENGTH_LONG).show()
    }
}

val imageCacheDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

fun imageExceptionHandler(
    imageLoadFail: ImageLoadFail,
    unknownFail: UnknownFail,
    ) = CoroutineExceptionHandler { _, throwable ->
        if (ConnectionManager.online) {
            when (throwable) {
                is ImageLoadException -> {
                    val (errorMessage, canBeLoad) = if (throwable.innerException == null) {
                        Log.e("Error", """Unable to load ${throwable.url} because "${throwable.message}" """)
                        (throwable.message ?: "Неизвестная ошибка") to throwable.validUrl
                    } else {
                        Log.e("Error", """Unable to load ${throwable.url} with exception ${throwable.innerException} (${throwable.localizedMessage}) """)
                        val canBeLoad = when (throwable.innerException) {
                            is FileNotFoundException -> { true }
                            is TimeoutException -> { true }
                            else -> { false }
                        }
                        (throwable.innerException.message ?: "Неизвестная ошибка") to canBeLoad
                    }

                    CacheManager.removeBothImages(throwable.url)

                    imageLoadFail(throwable.url, errorMessage, canBeLoad)
                }
                else -> {
                    Log.e("Error", throwable.toString())
                    unknownFail(throwable)
                }
            }
        }
}