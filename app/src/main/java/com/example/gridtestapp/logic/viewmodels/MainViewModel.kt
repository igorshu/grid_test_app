package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.None
import arrow.core.Option
import com.example.gridtestapp.core.ConnectionManager
import com.example.gridtestapp.core.Settings
import com.example.gridtestapp.logic.coroutines.ImageLoadFail
import com.example.gridtestapp.logic.coroutines.UnknownFail
import com.example.gridtestapp.logic.coroutines.imageExceptionHandler
import com.example.gridtestapp.logic.coroutines.showError
import com.example.gridtestapp.logic.events.ChangeVisibleIndexes
import com.example.gridtestapp.logic.events.DismissImageFailDialog
import com.example.gridtestapp.logic.events.LoadImageAgain
import com.example.gridtestapp.logic.events.MainEvent
import com.example.gridtestapp.logic.events.ShowImageFailDialog
import com.example.gridtestapp.logic.events.UpdateImageWidthEvent
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.states.MainScreenState
import com.example.gridtestapp.ui.cache.CacheManager
import com.example.gridtestapp.ui.cache.CacheManager.previewImageBitmap
import com.example.gridtestapp.ui.cache.MemoryManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.dsl.module
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
*
*   Вью модель для главного экрана с превьюшками
*
*/
class MainViewModel(private val application: Application): AndroidViewModel(application), KoinComponent, InetRestorer {

    private val handler = CoroutineExceptionHandler { _, exception -> showError(application, viewModelScope, exception) }
    private val imageCacheDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

    private val _state: MutableStateFlow<MainScreenState> = MutableStateFlow(MainScreenState.init())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    private val imageLoadFail: ImageLoadFail = { url, errorMessage, canBeLoad ->
        _state.update {
            val loadedUrls = it.urlStates.toMutableMap().apply { put(url, LoadState.FAIL) }
            val imageErrors = it.imageErrors.toMutableMap().apply { put(url, MainScreenState.ImageError(errorMessage, canBeLoad)) }
            it.copy(urlStates = loadedUrls, imageErrors = imageErrors)
        }
    }
    private val unknownFail: UnknownFail = { throwable ->
        viewModelScope.launch (handler) {
            Toast.makeText(application, throwable.message.toString(), Toast.LENGTH_LONG).show()
        }
    }
    private val _imageExceptionHandler = imageExceptionHandler(imageLoadFail, unknownFail)

    init {
        viewModelScope.launch(handler + Dispatchers.IO) {
            CacheManager.init(application)
            loadLinks()

            ConnectionManager
                .state
                .onEach { connectionState ->
                    Log.d("ConnectionManager", "connectionState = $connectionState")
                    if (!connectionState.previous && connectionState.current) {
                        restoreAfterDisconnect()
                    }
                }.collect()
        }
    }

    /*
    *
    *  Функция где мы восстанавливаем состояние Grid-а
    *  Если надо подгружаем список урлов
    *  Также подгружаем незагрузившиеся картинки в preload-е и на экране
    *
     */

    override fun restoreAfterDisconnect() {
        if (state.value.urls.isEmpty()) {
            viewModelScope.launch(handler + Dispatchers.IO) {
                loadLinks()
            }
            return
        }

        val newState = _state.updateAndGet {
            val urlStates = it.urlStates.entries.associate { entry ->
                if (entry.value == LoadState.LOADING || entry.value == LoadState.FAIL) {
                    entry.key to LoadState.IDLE
                } else {
                    entry.toPair()
                }
            }

            it.copy(urlStates = urlStates)
        }

        newState
            .urls
            .subList(state.value.preloadRange.first, state.value.preloadRange.last)
            .forEach { url ->
                tryLoadImage(url)
            }
    }

    private fun loadLinks() {
        val request: Request = Request.Builder()
            .url(MAIN_URL)
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

    private suspend fun loadImage(url: String) {
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

            MemoryManager.addBitmap(url, bitmap)

            _state.update {
                return@update it.copy(
                    urlStates = it.urlStates.toMutableMap().apply { put(url, LoadState.LOADED) }
                )
            }
        }
    }

    fun onEvent(event: MainEvent) {
        when (event) {
            is UpdateImageWidthEvent -> changeWidth(event.width)
            is ChangeVisibleIndexes -> {
                updateOuterImages(event.indexesOnScreen.sorted())
                if (event.index != null) {
                    val url = state.value.urls[event.index]
                    tryLoadImage(url)
                }
            }
            is ShowImageFailDialog ->  _state.update {  it.copy(showImageFailDialog = Option(event.url)) }
            is DismissImageFailDialog -> _state.update {  it.copy(showImageFailDialog = None) }
            is LoadImageAgain -> {
                _state.update {  it.copy(showImageFailDialog = None) }
                loadImageAgain(event)
            }
        }
    }

    private fun loadImageAgain(event: LoadImageAgain) {
        viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
            loadImage(event.url)
        }
    }

    private fun tryLoadImage(url: String) {
        viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
            if (!MemoryManager.exists(url)) {
                if (state.value.urlStates[url] == LoadState.IDLE) {
                    loadImage(url)
                }
            }
        }
    }

    private fun updateOuterImages(indexesOnScreen: List<Int>) {
        if (indexesOnScreen.isEmpty()) {
            return
        }

        val screenRange = indexesOnScreen.first()..indexesOnScreen.last()
        val preloadOffset = (indexesOnScreen.size * Settings.previewPreload / 2).roundToInt()
        val preloadRange = max(0, indexesOnScreen.first() - preloadOffset)..min(state.value.urls.size - 1, indexesOnScreen.last() + preloadOffset)

        val outerUrls = state.value.urls.filterIndexed {index, _ ->
            index !in preloadRange
        }

        _state.update {
            val urlStates = it.urlStates.toMutableMap()
            outerUrls.forEach { url ->
                MemoryManager.removeBitmap(url)

                if (urlStates[url] != LoadState.FAIL && urlStates[url] != LoadState.LOADING) {
                    urlStates[url] = LoadState.IDLE
                }
            }
            it.copy(urlStates = urlStates, screenRange = screenRange, preloadRange = preloadRange)
        }
    }

    private fun changeWidth(width: Int) {
        _state.update { it.copy(widthConsumed = true) }
        get<ImageWidth>().value = width
    }

    companion object {
        const val MAIN_URL: String = "https://it-link.ru/test/images.txt"
        val module = module {
            single { MainViewModel(get()) }
            single { ImageWidth(1) }
        }
    }
}