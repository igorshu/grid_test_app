package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.None
import arrow.core.Option
import com.example.gridtestapp.R
import com.example.gridtestapp.core.connection.ConnectionManager
import com.example.gridtestapp.core.NotificationsManager
import com.example.gridtestapp.core.Settings
import com.example.gridtestapp.logic.coroutines.ImageLoadFail
import com.example.gridtestapp.logic.coroutines.UnknownFail
import com.example.gridtestapp.logic.coroutines.imageCacheDispatcher
import com.example.gridtestapp.logic.coroutines.imageExceptionHandler
import com.example.gridtestapp.logic.coroutines.showError
import com.example.gridtestapp.logic.events.AppPaused
import com.example.gridtestapp.logic.events.AppResumed
import com.example.gridtestapp.logic.events.ChangeVisibleIndexes
import com.example.gridtestapp.logic.events.DismissImageFailDialog
import com.example.gridtestapp.logic.events.ImageScreenEvent
import com.example.gridtestapp.logic.events.LoadImageAgain
import com.example.gridtestapp.logic.events.MainScreenEvent
import com.example.gridtestapp.logic.events.OnAppEvent
import com.example.gridtestapp.logic.events.SharePressed
import com.example.gridtestapp.logic.events.ShowImageFailDialog
import com.example.gridtestapp.logic.events.ToggleFullScreen
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.states.MainScreenState
import com.example.gridtestapp.logic.states.Screen
import com.example.gridtestapp.core.cache.CacheManager
import com.example.gridtestapp.core.cache.MemoryManager
import com.example.gridtestapp.logic.events.ChangeTheme
import com.example.gridtestapp.logic.events.Reload
import com.example.gridtestapp.logic.events.RemoveImage
import com.example.gridtestapp.logic.events.UpdateCurrentImageUrl
import com.example.gridtestapp.logic.states.Theme
import com.example.gridtestapp.ui.navigation.Routes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.dsl.module
import org.koin.core.component.inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AppViewModel(private val application: Application): AndroidViewModel(application), KoinComponent {

    private val notificationsManager: NotificationsManager by inject()

    private var updateOuterPreviewsJob: Job? = null

    private val handler = CoroutineExceptionHandler { _, exception -> showError(application, viewModelScope, exception) }

    private val appName = application.getString(application.applicationInfo.labelRes)

    private val _state: MutableStateFlow<AppState> = MutableStateFlow(AppState.init(appName))
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val imageLoadFail: ImageLoadFail = { url, errorMessage, canBeLoad ->
        _state.update {
            val loadedUrls = it.previewUrlStates.toMutableMap().apply { put(url, LoadState.FAIL) }
            val imageErrors = it.imageErrors.toMutableMap().apply { put(url, MainScreenState.ImageError(errorMessage, canBeLoad)) }
            it.copy(previewUrlStates = loadedUrls, imageErrors = imageErrors)
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
            listenConnectionState()
            ConnectionManager.init(application)
        }
    }

    private suspend fun listenConnectionState() {
        ConnectionManager
            .state
            .onEach { connectionState ->
                Log.d("ConnectionManager", "connectionState = $connectionState")
                if (!connectionState.previous && connectionState.current) {
                    restoreAfterDisconnect()
                }
            }.collect()
    }

    private fun loadLinks() {
        val request: Request = Request.Builder()
            .url(MAIN_URL)
            .build()

        val txt = OkHttpClient().newCall(request).execute().use {
            response -> return@use response.body?.string() ?: throw Exception(application.getString(R.string.empty_answer))
        }

        val lines = txt.lines()

        _state.update { it.copy(urls = lines) }

        lines.forEach { url ->
            _state.update {
                it.copy(previewUrlStates = it.previewUrlStates.toMutableMap().apply { put(url, LoadState.IDLE) })
            }
        }
    }

    private suspend fun loadImage(url: String) {
        if (CacheManager.isNotCached(url)) {
            _state.update {
                it.copy(previewUrlStates = it.previewUrlStates.toMutableMap().apply { put(url, LoadState.LOADING) })
            }
            if (CacheManager.loadImage(url)) {
                setLoadedState(url)
            }
        } else {
            setLoadedState(url)
        }
    }

    private fun setLoadedState(url: String) {
        val bitmap: ImageBitmap? = CacheManager.previewImageBitmap(url)
        if (bitmap != null) {

            MemoryManager.addPreviewBitmap(url, bitmap)

            _state.update {
                return@update it.copy(
                    previewUrlStates = it.previewUrlStates.toMutableMap().apply { put(url, LoadState.LOADED) }
                )
            }
        }
    }

    private fun loadImageAgain(event: LoadImageAgain) {
        viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
            loadImage(event.url)
        }
    }

    private fun loadImageFromMemory(url: String) {
        viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
            if (!MemoryManager.previewExists(url)) {
                if (state.value.previewUrlStates[url] == LoadState.IDLE) {
                    loadImage(url)
                }
            }
        }
    }

    /*
    *
    * Здесь чистим превью картинки из памяти за пределеми preload-а
    *
     */

    private fun updateOuterPreviews(indexesOnScreen: List<Int>) {
        if (indexesOnScreen.isEmpty()) {
            return
        }

        val screenRange = indexesOnScreen.first()..indexesOnScreen.last()
        val preloadOffset = (indexesOnScreen.size * Settings.PREVIEW_PRELOAD / 2).roundToInt()
        val preloadRange = max(0, indexesOnScreen.first() - preloadOffset)..min(state.value.urls.size - 1, indexesOnScreen.last() + preloadOffset)

        val outerUrls = state.value.urls.filterIndexed {index, _ ->
            index !in preloadRange
        }

        _state.update {
            val urlStates = it.previewUrlStates.toMutableMap()
            outerUrls.forEach { url ->
                MemoryManager.removePreviewBitmap(url)

                if (urlStates[url] != LoadState.FAIL && urlStates[url] != LoadState.LOADING) {
                    urlStates[url] = LoadState.IDLE
                }
            }
            it.copy(previewUrlStates = urlStates, screenRange = screenRange, preloadRange = preloadRange)
        }
    }

    /*
    *
    *  Функция где мы восстанавливаем состояние Grid-а
    *  Если надо подгружаем список урлов
    *  Также подгружаем незагрузившиеся картинки в preload-е и на экране
    *
     */

    private fun restoreAfterDisconnect() {
        if (state.value.urls.isEmpty()) {
            viewModelScope.launch(handler + Dispatchers.IO) {
                loadLinks()
            }
            return
        }

        val newState = _state.updateAndGet {
            val urlStates = it.previewUrlStates.entries.associate { entry ->
                if (entry.value == LoadState.LOADING || entry.value == LoadState.FAIL) {
                    entry.key to LoadState.IDLE
                } else {
                    entry.toPair()
                }
            }

            it.copy(previewUrlStates = urlStates)
        }

        newState
            .urls
            .subList(state.value.preloadRange.first, state.value.preloadRange.last)
            .forEach { url ->
                loadImageFromMemory(url)
            }
    }

    val onEvent: OnAppEvent = { event ->
        when (event) {
            is ToggleFullScreen -> _state.update {
                if (it.currentScreen == Screen.IMAGE) { it.copy(showTopBar = !it.showTopBar, showSystemBars = !it.showSystemBars) } else { it }
            }
            is MainScreenEvent -> {
                _state.update {
                    it.copy(
                        showTopBar = true,
                        showSystemBars = true,
                        title = appName,
                        showBack = false,
                        currentScreen = Screen.MAIN,
                    )
                }
                notificationsManager.showAppNotification()
            }
            is ImageScreenEvent -> _state.update {
                it.copy(showTopBar = true,
                    showSystemBars = true,
                    title = event.url,
                    currentImageUrl = event.url,
                    showBack = true,
                    deletingImage = false,
                    currentScreen = Screen.IMAGE,
                )
            }
            is SharePressed -> shareUrl(event.url)
            is ShowImageFailDialog ->  _state.update {  it.copy(showImageFailDialog = Option(event.url)) }
            is DismissImageFailDialog -> _state.update {  it.copy(showImageFailDialog = None) }
            is LoadImageAgain -> {
                _state.update {  it.copy(showImageFailDialog = None) }
                loadImageAgain(event)
            }
            is ChangeVisibleIndexes -> {
                updateOuterPreviewsJob?.cancel()
                updateOuterPreviewsJob = viewModelScope.launch {
                    delay(100)
                    if (isActive) {
                        updateOuterPreviews(event.indexesOnScreen.sorted())
                    }
                }

                if (event.index != null) {
                    val url = state.value.urls[event.index]
                    loadImageFromMemory(url)
                }
            }
            is AppResumed -> notificationsManager.showResumeNotification()
            is AppPaused -> notificationsManager.hideNotification()
            is ChangeTheme -> _state.update { it.copy(theme = Theme.entries[event.index]) }
            is Reload -> reload()
            is RemoveImage -> removeImage(event.url)
            is UpdateCurrentImageUrl -> _state.update { it.copy(currentImageUrl = event.url) }
        }
    }

    private fun removeImage(url: String) {
        viewModelScope.launch(handler + Dispatchers.IO) {
            MemoryManager.removeBothImages(url)
            CacheManager.removeBothImages(url)
            _state.update {
                val urls = it.urls.toMutableList().apply { remove(url) }
                val previewUrlStates = it.previewUrlStates.toMutableMap().apply { remove(url) }
                it.copy(
                    urls = urls,
                    previewUrlStates = previewUrlStates,
                    deletingImage = true,
                )
            }

            viewModelScope.launch(handler + Dispatchers.Main) {
                get<Routes>().navigate(Routes.MAIN)
            }
        }
    }

    private fun reload() {
        viewModelScope.launch(handler + Dispatchers.IO) {
            MemoryManager.clearAll()
            CacheManager.clearAll()

            _state.update { it.clear() }
            loadLinks()
        }
    }

    private fun shareUrl(url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_TEXT, url)
        shareIntent.setType("text/plain")
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(shareIntent)
    }

    companion object {

        const val MAIN_URL: String = "https://it-link.ru/test/images.txt"

        val module = module {
            single { AppViewModel(get()) }
        }
    }

}