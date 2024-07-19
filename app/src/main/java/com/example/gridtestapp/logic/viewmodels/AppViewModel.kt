package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.toMutableStateList
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
import com.example.gridtestapp.logic.events.AppEvent
import com.example.gridtestapp.logic.events.AppPaused
import com.example.gridtestapp.logic.events.AppResumed
import com.example.gridtestapp.logic.events.ChangeTheme
import com.example.gridtestapp.logic.events.ChangeVisibleRange
import com.example.gridtestapp.logic.events.DismissImageFailDialog
import com.example.gridtestapp.logic.events.GoBackFromImage
import com.example.gridtestapp.logic.events.GotUrlIntent
import com.example.gridtestapp.logic.events.ImagePressed
import com.example.gridtestapp.logic.events.ImagePressedNavigate
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
import com.example.gridtestapp.logic.states.ImageState
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.states.Screen
import com.example.gridtestapp.logic.states.Theme
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.other.index
import com.example.gridtestapp.ui.other.notExists
import com.example.gridtestapp.ui.other.size
import com.example.gridtestapp.ui.other.urls
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    private var previewsJob: Job? = null
    private val notificationsManager: NotificationsManager by inject()
    private val imageLoader: ImageLoader by inject()
    private val localRepo: LocalRepo by inject()
    private val connectionManager: ConnectionManager by inject()

    private val handler = CoroutineExceptionHandler { _, exception -> showError(application, viewModelScope, exception) }

    private val appName = application.getString(application.applicationInfo.labelRes)

    private val _state: MutableStateFlow<AppState> = MutableStateFlow(AppState.init(appName))
    val state: StateFlow<AppState> = _state.asStateFlow()

    val themeFlow = state.map { it.theme }
    val systemBarsFlow = state.map { it.showSystemBars }

    private val _event: MutableSharedFlow<AppEvent> = MutableSharedFlow()


    private val imageLoadFail: ImageLoadFail = { url, errorMessage, canBeLoad ->
        _state.update {
            val imageStates = it.imageStates.toMutableList()
            val index = imageStates.index(url)
            imageStates[index] = ImageState(url, ImageError(errorMessage, canBeLoad), LoadState.FAIL, null)
            it.copy(imageStates = imageStates)
        }
    }

    private val unknownFail: UnknownFail = { throwable ->
        viewModelScope.launch (handler) {
            Toast.makeText(application, throwable.message.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private val _imageExceptionHandler = imageExceptionHandler(imageLoadFail, unknownFail, connectionManager)

    init {
        subscribeToEvents()

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

    private fun subscribeToEvents() {
        viewModelScope.launch {
            _event.collect {
                onEvent(it)
            }
        }
    }

    fun setEvent(event: AppEvent) {
        viewModelScope.launch { _event.emit(event) }
    }

    private fun loadLinks() {
        _state.update { it.copy(loading = true) }

        val urls = (localRepo.urls ?: run {
            val request: Request = Request.Builder()
                .url(MAIN_URL)
                .build()

            val txt = OkHttpClient().newCall(request).execute().use {
                    response -> return@use response.body?.string() ?: throw Exception(application.getString(R.string.empty_answer))
            }

            txt.lines().apply {
                localRepo.urls = this
            }
        }).toMutableStateList()

        val imageStates = urls.map { url -> ImageState( url, null, LoadState.IDLE, null) }

        _state.update { it.copy(loading = false, imageStates = imageStates) }
    }

    private suspend fun loadImage(url: String, index: Int) {
        imageLoader.loadImage(
            url,
            onLoading = { url ->
                _state.update {
                    val imageStates = _state.value.imageStates.toMutableList()
                    imageStates[index] = ImageState(url, null, LoadState.LOADING, null)

                    it.copy(imageStates = imageStates)
                }
            },
            onLoaded = { url ->
                setLoadedState(url, index)
            }
        )
    }

    private fun setLoadedState(url: String, index: Int) {
        val bitmap: ImageBitmap? = CacheManager.previewImageBitmap(url)
        if (bitmap != null) {

            _state.update {
                val imageStates = _state.value.imageStates.toMutableList()
                imageStates[index] = ImageState(url, null, LoadState.LOADED, bitmap.apply { prepareToDraw() })
                it.copy(imageStates = imageStates)
            }
        }
    }

    private fun loadImageAgain(event: LoadImageAgain) {
        viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
            loadImage(event.url, event.index)
        }
    }

    private fun loadPreviewFromMemory(index: Int, url: String) {
        if (_state.value.imageStates.notExists(url)) {
            val imageStates = _state.value.imageStates
            if (imageStates[index].previewState == LoadState.IDLE) {
                viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
                    loadImage(url, index)
                }
            }
        } // else не нужен так как если preview exists состояние уже LOADED
    }

    /*
    *
    * Здесь чистим превью картинки из памяти за пределеми preload-а, и подгружаем внутри
    *
     */

    private fun handlePreviews(screenRange: IntRange, scope: CoroutineScope) {
//        Log.d("range", "start")

        val size = _state.value.imageStates.size
        if (screenRange.isEmpty() || size == 0) {
            return
        }

        val preloadOffset = (screenRange.size() * Settings.PREVIEW_PRELOAD / 2).roundToInt()
        val preloadRange = max(0, screenRange.first() - preloadOffset)..min(size - 1, screenRange.last() + preloadOffset)

//        Log.d("range", "preloadRange = $preloadRange")

        _state.update {
            val imageStates = _state.value.imageStates.toMutableList()

            imageStates.forEachIndexed { index, imageState ->
                if (scope.isActive) {
                    val previewState = imageState.previewState
                    if (index in preloadRange) { // inner
//                    Log.d("range", "$index. inner, isActive = ${scope.isActive}")

                        if (previewState == LoadState.IDLE) {
//                        Log.d("range", "$index. load, previewState = $previewState")
                            viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
                                loadImage(imageState.url, index)
                            }
                        } else {
//                        Log.d("range", "$index. skip, previewState = $previewState")
                        }
                    } else { // outer
//                    Log.d("range", "$index. outer, isActive = ${scope.isActive}")

                        if (previewState != LoadState.FAIL && previewState != LoadState.LOADING) {
                            val url = imageState.url
                            imageStates[index] = ImageState(url, null, LoadState.IDLE, null)
                        }
                    }
                } else {
                    Log.e("range", "is NOT Active, return, preloadRange = $preloadRange")
                    return
                }
            }

//            Log.d("range", "_update, imStates = ${imageStates.hashCode()}")
            it.copy(
                imageStates = imageStates,
                screenRange = screenRange,
                preloadRange = preloadRange,
            )
        }

//        Log.d("range", "end, preloadRange = $preloadRange")
    }

    /*
    *
    *  Функция где мы восстанавливаем состояние Grid-а
    *  Если надо подгружаем список урлов
    *  Также подгружаем незагрузившиеся картинки в preload-е и на экране
    *
     */

    private fun restoreAfterDisconnect() {
        if (_state.value.imageStates.isEmpty()) {
            viewModelScope.launch(handler + Dispatchers.IO) {
                loadLinks()
            }
            return
        }

        _state.value.imageStates.forEachIndexed { index, imageState ->
            if (imageState.previewState == LoadState.LOADING || imageState.previewState == LoadState.FAIL) {
                _state.update {
                    val imageStates = _state.value.imageStates.toMutableList()
                    imageStates[index] = ImageState(imageState.url, null, LoadState.IDLE, null)
                    it.copy(imageStates = imageStates)
                }

            }
        }

        _state
            .value
            .imageStates
            .subList(state.value.preloadRange.first, state.value.preloadRange.last)
            .forEachIndexed { index, imageState ->
                viewModelScope.launch(_imageExceptionHandler + imageCacheDispatcher) {
                    loadImage(imageState.url, index)
                }
            }
    }

    private val onEvent: OnAppEvent = { event ->
        Log.d("event", "event = $event")
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
            is ChangeVisibleRange -> {
                previewsJob?.cancel()
//                Log.d("range", "cancel")
                previewsJob = viewModelScope.launch(handler + Dispatchers.Default) {
                    handlePreviews(event.visibleRange, this)
                }
            }
            is AppResumed -> notificationsManager.showResumeNotification()
            is AppPaused -> notificationsManager.hideNotification()
            is ChangeTheme -> changeTheme(event)
            is Reload -> reload()
            is RemoveImage -> removeImage(event.url, event.index)
            is UpdateCurrentImage -> _state.update {
                it.copy(
                    currentImage = AppState.ImagePair(event.url, event.index),
                    selectedImage = AppState.ImageSelected(event.url, event.index, true),
                    title = event.url,
                )
            }
            is GotUrlIntent -> navigateToAddImage(event.url)
            is AddImage -> addImageToTop(event.url)
            is ImagePressed -> imagePressed(event.url, event.index)
            is ImagePressedNavigate -> imagePressedNavigate(event.url, event.index)
            is GoBackFromImage -> {
                _state.update { it.copy(hideImage = true) }
                get<Routes>().goBack()
            }
        }
    }

    private fun imagePressed(url: String, index: Int) {
        _state.update { it.copy(selectedImage = AppState.ImageSelected(url, index, false)) }
    }

    private fun imagePressedNavigate(url: String, index: Int) {
        _state.update { it.copy(selectedImage = AppState.ImageSelected(url, index, true)) }
        viewModelScope.launch {
            delay(100)
            if (!get<Routes>().isImage()) {
                get<Routes>().navigate(Routes.imageRoute(url, index))
            }
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

            if (CacheManager.isCached(url)) {
                val bitmap = CacheManager.previewImageBitmap(url)
                if (bitmap != null) {
                    _state.update {
                        val imageStates = it.imageStates.toMutableList()
                        imageStates.apply {
                            val index = index(url)
                            removeAt(index)
                            add(0, ImageState(url, null, LoadState.LOADED, bitmap.apply { prepareToDraw() }))
                        }
                        it.copy(imageStates = imageStates)
                    }
                }
            }

            localRepo.urls = _state.value.imageStates.urls()

            if (state.value.currentScreen == Screen.ADD_IMAGE) {
                viewModelScope.launch(handler + Dispatchers.Main) {
                    get<Routes>().goBack()
                }
            }
        }
    }

    private fun removeImage(url: String, index: Int) {
        viewModelScope.launch(handler + Dispatchers.IO) {

            _state.update {
                val imageStates = it.imageStates.toMutableList().apply { removeAt(index) }
                it.copy(imageStates = imageStates, hideImage = true)
            }

            MemoryManager.removeBothImages(url)
            CacheManager.removeBothImages(url)

            localRepo.urls = _state.value.imageStates.urls()

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