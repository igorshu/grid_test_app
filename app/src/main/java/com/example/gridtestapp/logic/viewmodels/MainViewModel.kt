package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridtestapp.logic.events.ChangeVisibleIndexes
import com.example.gridtestapp.logic.events.MainEvent
import com.example.gridtestapp.logic.events.UpdateImageWidthEvent
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.states.MainScreenState
import com.example.gridtestapp.ui.cache.CacheManager
import com.example.gridtestapp.ui.cache.CacheManager.previewImageBitmap
import com.example.gridtestapp.ui.exceptions.ImageLoadException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.dsl.module
import java.util.HashSet
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
*
*   Вью модель для главоного экрана с превьюшками
*
*/
class MainViewModel(private val application: Application): AndroidViewModel(application),
    KoinComponent {

    private val handler = CoroutineExceptionHandler { _, exception -> showError(exception)}
    private val imageExceptionHandler = CoroutineExceptionHandler { _, exception -> loadImageError(exception)}
    private val imageDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

    private val preload = 0.5f

    private fun loadImageError(throwable: Throwable) {
        if (throwable is ImageLoadException) {
            Log.e("Error", """Unable to load ${throwable.url} because "${throwable.message}" """)

            CacheManager.removeBothImages(throwable.url)

            _state.update {
                val url = throwable.url
                val loadedUrls = it.urlStates.toMutableMap().apply { put(url, LoadState.FAIL) }
                it.copy(urlStates = loadedUrls)
            }
        } else {
            Log.e("Error", throwable.message.toString())

            viewModelScope.launch {
                Toast.makeText(application, throwable.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showError(throwable: Throwable) {
        viewModelScope.launch {
            Toast.makeText(application, throwable.message, Toast.LENGTH_LONG).show()
        }
    }

    private val _state: MutableStateFlow<MainScreenState> = MutableStateFlow(MainScreenState(
        urls = listOf(),
        previewBitmaps = hashMapOf(),
        urlStates = hashMapOf(),
        widthConsumed = false
        ))
    val state: StateFlow<MainScreenState> = _state.asStateFlow()


    init {
        viewModelScope.launch(handler + Dispatchers.IO) {
            CacheManager.init(application)
            loadLinks()
        }
    }

    private fun loadLinks() {
        val request: Request = Request.Builder()
            .url("https://it-link.ru/test/images.txt")
            .build()

        val txt = OkHttpClient().newCall(request).execute().use {
            response -> return@use response.body!!.string()
        }

        val lines = txt.lines()

        _state.update { it.copy(urls = lines) }

        lines.forEach { url ->
            _state.update {
                it.copy(urlStates = it.urlStates.toMutableMap().apply { put(url, LoadState.IDLE) })
            }
        }
    }

    private suspend fun loadImage(url: String, removeBefore: Boolean) {
        if (removeBefore) {
            CacheManager.removeBothImages(url)
        }

        if (CacheManager.isNotCached(url)) {
            _state.update {
                it.copy(urlStates = it.urlStates.toMutableMap().apply { put(url, LoadState.LOADING) })
            }

            if (CacheManager.loadImage(url)) {
                updateLoadedState(url)
            }
        } else {
            updateLoadedState(url)
        }
    }

    private fun updateLoadedState(url: String) {
        val bitmap: ImageBitmap? = previewImageBitmap(url)
        if (bitmap != null) {
            _state.update {
                val previewBitmaps = it.previewBitmaps.toMutableMap()
                previewBitmaps[url] = bitmap
                return@update it.copy(
                    previewBitmaps = previewBitmaps.toMap(),
                    urlStates = it.urlStates.toMutableMap().apply { put(url, LoadState.LOADED) }
                )
            }
        }
    }

    fun onEvent(event: MainEvent) {
        when (event) {
            is UpdateImageWidthEvent -> changeWidth(event.width)
            is ChangeVisibleIndexes -> updateImagesList(event.indexes.sorted())
        }
    }

    private fun updateImagesList(indexesOnScreen: List<Int>) {
        if (indexesOnScreen.isEmpty()) {
            return
        }
        viewModelScope.launch(imageExceptionHandler + imageDispatcher) {

            val preloadOffset = (indexesOnScreen.size * preload / 2).roundToInt()
            val preloadRange = max(0, indexesOnScreen.first() - preloadOffset)..min(state.value.urls.size - 1, indexesOnScreen.last() + preloadOffset)

            val outerUrls = state.value.urls.filterIndexed {index, _ ->
                index !in preloadRange
            }

            _state.update {
                val previewBitmaps = it.previewBitmaps.toMutableMap()
                val urlStates = it.urlStates.toMutableMap()
                outerUrls.forEach { url ->
                    previewBitmaps.remove(url)
                    urlStates[url] = LoadState.IDLE
                }
                it.copy(previewBitmaps = previewBitmaps, urlStates = urlStates)
            }

            preloadRange.forEach { index ->
                val url = state.value.urls[index]
                if (state.value.urlStates[url] == LoadState.IDLE) {
                    loadImage(url, false)
                }
            }
        }
    }

    private fun changeWidth(width: Int) {
        _state.update { it.copy(widthConsumed = true) }
        get<ImageWidth>().value = width
    }

    companion object {
        val module = module {
            single { MainViewModel(get()) }
            single { ImageWidth(1) }
        }
    }
}