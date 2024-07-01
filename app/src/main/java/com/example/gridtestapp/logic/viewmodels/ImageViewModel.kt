package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridtestapp.core.Settings
import com.example.gridtestapp.logic.coroutines.ImageLoadFail
import com.example.gridtestapp.logic.coroutines.UnknownFail
import com.example.gridtestapp.logic.coroutines.imageCacheDispatcher
import com.example.gridtestapp.logic.coroutines.imageExceptionHandler
import com.example.gridtestapp.logic.coroutines.showError
import com.example.gridtestapp.logic.events.LoadOriginalImageFromDisk
import com.example.gridtestapp.logic.events.OnImageEvent
import com.example.gridtestapp.logic.states.ImageScreenState
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.ui.cache.CacheManager
import com.example.gridtestapp.ui.cache.MemoryManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/*
*
*   Вью модель для экрана с картинкой
*
*/
class ImageViewModel(
    private val urls: List<String>,
    private val application: Application,
    initial: Pair<Int, String>
): AndroidViewModel(application) {

    private val handler = CoroutineExceptionHandler { _, exception -> showError(application, viewModelScope, exception)}

    private val _state: MutableStateFlow<ImageScreenState> = MutableStateFlow(ImageScreenState.init(initialIndex = initial.first))
    val state: StateFlow<ImageScreenState> = _state.asStateFlow()

    private val imageLoadFail: ImageLoadFail = { url, errorMessage, canBeLoad -> }
    private val unknownFail: UnknownFail = { throwable ->
        viewModelScope.launch (handler) {
            Toast.makeText(application, throwable.message.toString(), Toast.LENGTH_LONG).show()
        }
    }
    private val _imageExceptionHandler = imageExceptionHandler(imageLoadFail, unknownFail)

    init {
        loadOriginalImageFromDisk(initial.first, initial.second)
    }

    val onEvent: OnImageEvent = { event ->
        when (event) {
            is LoadOriginalImageFromDisk -> {
                loadOriginalImageFromDisk(event.index, event.url)
            }
        }
    }

    private fun loadOriginalImageFromDisk(index: Int, url: String) {
        viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
            if (CacheManager.isCached(url)) {
                val bitmap = CacheManager.originalImageBitmap(url)
                _state.update {
                    MemoryManager.addOriginalBitmap(url, bitmap)

                    val originalUrlStates = it.originalUrlStates.toMutableMap()
                    originalUrlStates[url] = LoadState.LOADED

                    urls.filterIndexed() { i, url ->
                        i < index - Settings.originalPreloadOffset || i > index + Settings.originalPreloadOffset
                    }
                    .forEach { url ->
                        MemoryManager.removeOriginalBitmap(url)
                        originalUrlStates[url] = LoadState.IDLE
                    }

                    it.copy(originalUrlStates = originalUrlStates)
                }
            } else {
                _state.update {
                    val originalUrlStates = it.originalUrlStates.toMutableMap().apply { put(url, LoadState.LOADING) }
                    it.copy(originalUrlStates = originalUrlStates)
                }
                if (CacheManager.loadImage(url)) {
                    val bitmap = CacheManager.originalImageBitmap(url)
                    MemoryManager.addOriginalBitmap(url, bitmap)
                    _state.update {
                        val originalUrlStates = it.originalUrlStates.toMutableMap().apply { put(url, LoadState.LOADED) }
                        it.copy(originalUrlStates = originalUrlStates)
                    }
                }
            }
        }
    }
}