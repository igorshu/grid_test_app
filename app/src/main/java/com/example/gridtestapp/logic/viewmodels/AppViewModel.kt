package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.None
import arrow.core.Option
import com.example.gridtestapp.R
import com.example.gridtestapp.core.LocalRepo
import com.example.gridtestapp.core.NotificationsManager
import com.example.gridtestapp.core.Settings
import com.example.gridtestapp.core.cache.CacheManager
import com.example.gridtestapp.core.cache.ImageLoader
import com.example.gridtestapp.core.cache.MemoryManager
import com.example.gridtestapp.core.connection.ConnectionManager
import com.example.gridtestapp.logic.coroutines.ImageLoadFail
import com.example.gridtestapp.logic.coroutines.UnknownFail
import com.example.gridtestapp.logic.coroutines.imageCacheDispatcher
import com.example.gridtestapp.logic.coroutines.imageExceptionHandler
import com.example.gridtestapp.logic.coroutines.showError
import com.example.gridtestapp.logic.events.AddImage
import com.example.gridtestapp.logic.events.AddImageScreenEvent
import com.example.gridtestapp.logic.events.AppPaused
import com.example.gridtestapp.logic.events.AppResumed
import com.example.gridtestapp.logic.events.ChangeTheme
import com.example.gridtestapp.logic.events.ChangeVisibleIndexes
import com.example.gridtestapp.logic.events.DismissImageFailDialog
import com.example.gridtestapp.logic.events.GotUrlIntent
import com.example.gridtestapp.logic.events.ImageScreenEvent
import com.example.gridtestapp.logic.events.LoadImageAgain
import com.example.gridtestapp.logic.events.MainScreenEvent
import com.example.gridtestapp.logic.events.OnAppEvent
import com.example.gridtestapp.logic.events.Reload
import com.example.gridtestapp.logic.events.RemoveImage
import com.example.gridtestapp.logic.events.SharePressed
import com.example.gridtestapp.logic.events.ShowImageFailDialog
import com.example.gridtestapp.logic.events.ToggleFullScreen
import com.example.gridtestapp.logic.events.UpdateCurrentImage
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.states.ImageError
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.states.Screen
import com.example.gridtestapp.logic.states.Theme
import com.example.gridtestapp.ui.navigation.Routes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.dsl.module
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AppViewModel(private val application: Application): AndroidViewModel(application), KoinComponent {

    private val notificationsManager: NotificationsManager by inject()
    private val imageLoader: ImageLoader by inject()
    private val localRepo: LocalRepo by inject()
    private val connectionManager: ConnectionManager by inject()

    private var updateOuterPreviewsJob: Job? = null

    private val handler = CoroutineExceptionHandler { _, exception -> showError(application, viewModelScope, exception) }

    private val appName = application.getString(application.applicationInfo.labelRes)

    private val _state: MutableStateFlow<AppState> = MutableStateFlow(AppState.init(appName))
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val imageLoadFail: ImageLoadFail = { url, errorMessage, canBeLoad ->
        _state.update {
            val loadedUrls = it.previewUrlStates.toMutableMap().apply { put(url, LoadState.FAIL) }
            val imageErrors = it.imageErrors.toMutableMap().apply { put(url, ImageError(errorMessage, canBeLoad)) }
            it.copy(previewUrlStates = loadedUrls, imageErrors = imageErrors)
        }
    }

    private val unknownFail: UnknownFail = { throwable ->
        viewModelScope.launch (handler) {
            Toast.makeText(application, throwable.message.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private val _imageExceptionHandler = imageExceptionHandler(imageLoadFail, unknownFail, connectionManager)

    init {
        viewModelScope.launch {
            connectionManager.listen { restoreAfterDisconnect() }
        }

        viewModelScope.launch(handler + Dispatchers.IO) {
            CacheManager.init(application)
            loadLinks()
            connectionManager.init()
        }

        _state.update { it.copy(theme = Theme.entries[localRepo.theme]) }
    }

    private fun loadLinks() {
        val urls = localRepo.urls ?: run {
            val request: Request = Request.Builder()
                .url(MAIN_URL)
                .build()

            val txt = OkHttpClient().newCall(request).execute().use {
                    response -> return@use response.body?.string() ?: throw Exception(application.getString(R.string.empty_answer))
            }

            txt.lines().apply {
                localRepo.urls = this
            }
        }

        _state.update { it.copy(urls = urls) }

        urls.forEach { url ->
            _state.update {
                it.copy(previewUrlStates = it.previewUrlStates.toMutableMap().apply { put(url, LoadState.IDLE) })
            }
        }
    }

    private suspend fun loadImage(url: String) {
        imageLoader.loadImage(
            url,
            onLoading = { url ->
                _state.update {
                    it.copy(previewUrlStates = it.previewUrlStates.toMutableMap().apply { put(url, LoadState.LOADING) })
                }
            },
            onLoaded = { url ->
                setLoadedState(url)
            }
        )
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

    private fun loadPreviewFromMemory(url: String) {
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

        var outerImageUrls = state.value.urls.filterIndexed { index, url ->
            index !in preloadRange
        }

        val innerImageUrls = state.value.urls.slice(preloadRange)
        outerImageUrls = outerImageUrls.filterNot { url ->
            url in innerImageUrls
        }

        _state.update {
            val urlStates = it.previewUrlStates.toMutableMap()
            outerImageUrls.forEach { url ->
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
                loadPreviewFromMemory(url)
            }
    }

    val onEvent: OnAppEvent = { event ->
//        Log.d("AppViewModel.onEvent", event.toString())
        when (event) {
            is ToggleFullScreen -> _state.update {
                if (it.currentScreen in setOf(Screen.IMAGE, Screen.ADD_IMAGE)) {
                    it.copy(showTopBar = !it.showTopBar, showSystemBars = !it.showSystemBars)
                } else { it }
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
                it.copy(
                    showTopBar = true,
                    showSystemBars = true,
                    title = event.url,
                    currentImage = AppState.ImagePair(event.url, event.index),
                    showBack = true,
                    hideImage = false,
                    currentScreen = Screen.IMAGE,
                )
            }
            is AddImageScreenEvent -> _state.update {
                it.copy(
                    showTopBar = true,
                    showSystemBars = true,
                    title = event.url,
                    showBack = true,
                    hideImage = true,
                    currentScreen = Screen.ADD_IMAGE,
                )
            }
            is SharePressed -> shareUrl(event.url)
            is ShowImageFailDialog ->  _state.update {  it.copy(showImageFailDialog = Option(event.url)) }
            is DismissImageFailDialog -> _state.update {  it.copy(showImageFailDialog = None) }
            is LoadImageAgain -> {
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
                    loadPreviewFromMemory(url)
                }
            }
            is AppResumed -> notificationsManager.showResumeNotification()
            is AppPaused -> notificationsManager.hideNotification()
            is ChangeTheme -> changeTheme(event)
            is Reload -> reload()
            is RemoveImage -> removeImage(event.url, event.index)
            is UpdateCurrentImage -> _state.update { it.copy(currentImage = AppState.ImagePair(event.url, event.index)) }
            is GotUrlIntent -> navigateToAddImage(event.url)
            is AddImage -> addImageToTop(event.url)
        }
    }

    private fun changeTheme(event: ChangeTheme) {
        localRepo.theme = event.index
        _state.update { it.copy(theme = Theme.entries[event.index]) }
    }

    private fun navigateToAddImage(url: String) {
        viewModelScope.launch(handler + Dispatchers.Main) {
            get<Routes>().replaceToMain(Routes.addImageRoute(url))
        }
    }

    private fun addImageToTop(url: String) {
        viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
            val newState = _state.updateAndGet {
                val urls = it.urls.toMutableList().apply {
                    remove(url)
                    add(0, url)
                }
                it.copy(urls = urls)
            }

            if (CacheManager.isCached(url)) {
                val bitmap = CacheManager.previewImageBitmap(url)
                if (bitmap != null) {
                    MemoryManager.addPreviewBitmap(url, bitmap)

                    _state.update {
                        val previewUrlStates = it.previewUrlStates.toMutableMap().apply {
                            put(url, LoadState.LOADED)
                        }
                        it.copy(previewUrlStates = previewUrlStates)
                    }
                }
            }

            localRepo.urls = newState.urls

            if (state.value.currentScreen == Screen.ADD_IMAGE) {
                viewModelScope.launch(handler + Dispatchers.Main) {
                    get<Routes>().goBack()
                }
            }
        }
    }

    private fun removeImage(url: String, index: Int) {
        viewModelScope.launch(handler + Dispatchers.IO) {
            val newState = _state.updateAndGet {
                val urls = it.urls.toMutableList().apply { removeAt(index = index) }
                it.copy(urls = urls)
            }

            if (newState.urls.indexOf(url) < 0) {
                MemoryManager.removeBothImages(url)
                CacheManager.removeBothImages(url)
                _state.update {
                    val previewUrlStates = it.previewUrlStates.toMutableMap().apply { remove(url) }
                    it.copy(
                        urls = newState.urls,
                        previewUrlStates = previewUrlStates,
                        hideImage = true)
                }
            }

            localRepo.urls = newState.urls

            viewModelScope.launch(handler + Dispatchers.Main) {
                get<Routes>().navigate(Routes.MAIN)
            }
        }
    }

    private fun reload() {
        viewModelScope.launch(handler + Dispatchers.IO) {
            MemoryManager.clearAll()
            CacheManager.clearAll()
            localRepo.clearUrls()

            _state.update { it.clear() }
            loadLinks()
        }
    }

    private fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }
        application.packageManager.queryIntentActivities(intent, 0)
            .filter { resolveInfo ->
                resolveInfo.activityInfo.packageName == application.packageName
            }.map { resolveInfo ->
                resolveInfo.activityInfo
            }.firstOrNull()?.let {
                val intentChooser = Intent.createChooser(intent, application.getString(R.string.share)).apply {
                    putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(it.packageName, it.name)))
                    setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                application.startActivity(intentChooser)
            }
        }

    companion object {

        const val MAIN_URL: String = "https://it-link.ru/test/images.txt"

        val module = module {
            single { AppViewModel(get()) }
        }
    }

    override fun onCleared() {
        Log.d("appViewModel", "onCleared")
        super.onCleared()
    }
}