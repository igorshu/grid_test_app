package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.content.res.Resources
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridtestapp.core.NotificationsManager
import com.example.gridtestapp.core.Settings
import com.example.gridtestapp.logic.coroutines.ImageLoadFail
import com.example.gridtestapp.logic.coroutines.UnknownFail
import com.example.gridtestapp.logic.coroutines.imageCacheDispatcher
import com.example.gridtestapp.logic.coroutines.imageExceptionHandler
import com.example.gridtestapp.logic.coroutines.showError
import com.example.gridtestapp.logic.events.LoadOriginalImageFromDisk
import com.example.gridtestapp.logic.events.OnImageEvent
import com.example.gridtestapp.logic.events.ShowImageNotification
import com.example.gridtestapp.logic.states.ImageScreenState
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.core.cache.CacheManager
import com.example.gridtestapp.core.cache.MemoryManager
import com.example.gridtestapp.core.connection.ConnectionManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

/*
*
*   Вью модель для экрана с картинкой
*
*/
class ImageViewModel(
    private val urls: List<String>,
    private val application: Application,
    val index: Int,
    val url: String,
): AndroidViewModel(application), KoinComponent {

    private val notificationsManager: NotificationsManager by inject()
    private val connectionManager: ConnectionManager by inject()

    private val handler = CoroutineExceptionHandler { _, exception -> showError(application, viewModelScope, exception)}

    private val _state: MutableStateFlow<ImageScreenState> = MutableStateFlow(ImageScreenState(initialIndex = index))
    val state: StateFlow<ImageScreenState> = _state.asStateFlow()

    private val imageLoadFail: ImageLoadFail = { _, _, _ -> }
    private val unknownFail: UnknownFail = { throwable ->
        viewModelScope.launch (handler) {
            Toast.makeText(application, throwable.message.toString(), Toast.LENGTH_LONG).show()
        }
    }
    private val _imageExceptionHandler = imageExceptionHandler(imageLoadFail, unknownFail, connectionManager)

    init {
        loadOriginalImageFromDisk(index, url)
    }

    val onEvent: OnImageEvent = { event ->
        when (event) {
            is LoadOriginalImageFromDisk -> {
                loadOriginalImageFromDisk(event.index, event.url)
            }
            is ShowImageNotification -> {
                viewModelScope.launch(handler + Dispatchers.Default) {
                    val imageBitmap = MemoryManager
                        .notificationBitmap(event.url, Resources.getSystem().displayMetrics.widthPixels)
                        ?.apply { prepareToDraw() }
                    notificationsManager.showImageNotification(imageBitmap, event.url)
                }
            }
        }
    }

    private fun loadOriginalImageFromDisk(index: Int, url: String) {
        viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
            if (CacheManager.isCached(url)) {
                val bitmap = CacheManager.originalImageBitmap(url)
                if (bitmap != null) {
                    _state.update {
                        val originalUrlStates = it.originalUrlStates.toMutableMap()
                        urls.filterIndexed { i, _ ->
                            i < index - Settings.ORIGINAL_PRELOAD_OFFSET || i > index + Settings.ORIGINAL_PRELOAD_OFFSET
                        }
                        .forEach { url ->
                            MemoryManager.removeOriginalBitmap(url)
                            originalUrlStates[url] = LoadState.IDLE
                        }

                        MemoryManager.addOriginalBitmap(url, bitmap)
                        originalUrlStates[url] = LoadState.LOADED

                        it.copy(originalUrlStates = originalUrlStates)
                    }
                } else {
                    _state.update {
                        val originalUrlStates = it.originalUrlStates.toMutableMap()
                        originalUrlStates[url] = LoadState.FAIL

                        it.copy(originalUrlStates = originalUrlStates)
                    }
                }
            } else {
                _state.update {
                    val originalUrlStates = it.originalUrlStates.toMutableMap().apply { put(url, LoadState.LOADING) }
                    it.copy(originalUrlStates = originalUrlStates)
                }
                if (CacheManager.loadImage(url)) {
                    val bitmap = CacheManager.originalImageBitmap(url)
                    if (bitmap != null) {
                        bitmap.prepareToDraw()
                        MemoryManager.addOriginalBitmap(url, bitmap)
                        _state.update {
                            val originalUrlStates = it.originalUrlStates.toMutableMap().apply { put(url, LoadState.LOADED) }
                            it.copy(originalUrlStates = originalUrlStates)
                        }
                    }  else {
                        _state.update {
                            val originalUrlStates = it.originalUrlStates.toMutableMap()
                            originalUrlStates[url] = LoadState.FAIL

                            it.copy(originalUrlStates = originalUrlStates)
                        }
                    }
                }
            }
        }
    }

    companion object {
        val module = module {
            viewModel { params -> ImageViewModel(urls = params.get(), get(), index = params.get(), url = params.get()) }
        }
    }
}