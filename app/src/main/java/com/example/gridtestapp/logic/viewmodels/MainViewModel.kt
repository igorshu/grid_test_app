package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.None
import arrow.core.Option
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
import com.example.gridtestapp.ui.exceptions.ImageLoadException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.dsl.module
import java.io.FileNotFoundException
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import javax.net.ssl.HttpsURLConnection
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
*
*   Вью модель для главного экрана с превьюшками
*
*/
class MainViewModel(private val application: Application): AndroidViewModel(application),
    KoinComponent {

    private val handler = CoroutineExceptionHandler { _, exception -> showError(exception)}
    private val imageExceptionHandler = CoroutineExceptionHandler { _, exception -> loadImageError(exception)}
    private val imageCacheDispatcher = Executors.newFixedThreadPool(4).asCoroutineDispatcher()

    private val preload = 0.5f

    private fun loadImageError(throwable: Throwable) {
        if (state.value.inetAvailable) {
            when (throwable) {
                is ImageLoadException -> {
                    val (errorMessage, canBeLoad) = if (throwable.innerException == null) {
                        Log.e("Error", """Unable to load ${throwable.url} because "${throwable.message}" """)
                        throwable.message!! to throwable.validUrl
                    } else {
                        Log.e("Error", """Unable to load ${throwable.url} with exception ${throwable.innerException} (${throwable.localizedMessage}) """)
                        val canBeLoad = when (throwable.innerException) {
                            is FileNotFoundException -> {
                                true
                            }
                            is TimeoutException -> {
                                true
                            }
                            else -> {
                                false
                            }
                        }
                        (throwable.innerException.message ?: "Неизвестная ошибка") to canBeLoad
                    }

                    CacheManager.removeBothImages(throwable.url)

                    _state.update {
                        val url = throwable.url
                        val loadedUrls = it.urlStates.toMutableMap().apply { put(url, LoadState.FAIL) }
                        val imageErrors = it.imageErrors.toMutableMap().apply { put(url, MainScreenState.ImageError(errorMessage, canBeLoad)) }
                        it.copy(urlStates = loadedUrls, imageErrors = imageErrors)
                    }
                }
                else -> {
                    Log.e("Error", throwable.toString())
                    viewModelScope.launch (handler) {
                        Toast.makeText(application, throwable.message.toString(), Toast.LENGTH_LONG).show()
                    }
                }
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
        urlStates = hashMapOf(),
        widthConsumed = false,
        inetAvailable = true,
        screenRange = IntRange(0, 0),
        preloadRange = IntRange(0, 0),
        showImageFailDialog = None,
        imageErrors = hashMapOf()
        ))
    val state: StateFlow<MainScreenState> = _state.asStateFlow()


    init {
        viewModelScope.launch(handler + Dispatchers.IO) {
            initConnectivity()
            CacheManager.init(application)
            loadLinks()
            startPinger()
        }
    }

    private fun startPinger() {
        viewModelScope.launch(handler + Dispatchers.IO) {
            while (true) {
                delay(15_000) // 15 seconds
                checkSiteUrl()
            }
        }
    }

    private fun checkSiteUrl() {
        try {
            val url = URL(MAIN_URL)
            val connection: HttpsURLConnection = url.openConnection() as HttpsURLConnection
            connection.connect()
            connection.disconnect()

            if (!state.value.inetAvailable) {
                _state.update {  it.copy(inetAvailable = true) }
                restoreAfterDisconnect()
            }
        } catch (e: Exception) {
            _state.update {  it.copy(inetAvailable = false) }
        }
    }

    private fun initConnectivity() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                _state.update { it.copy(inetAvailable = true) }
                restoreAfterDisconnect()
            }

            override fun onUnavailable() {
                super.onUnavailable()

                _state.update { it.copy(inetAvailable = false) }
            }

            override fun onLost(network: Network) {
                super.onLost(network)

                _state.update { it.copy(inetAvailable = false) }
            }
        }

        val connectivityManager = getSystemService(application, ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)
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
        viewModelScope.launch(imageExceptionHandler + imageCacheDispatcher) {
            loadImage(event.url)
        }
    }

    private fun tryLoadImage(url: String) {
        viewModelScope.launch(imageExceptionHandler + imageCacheDispatcher) {
            if (MemoryManager.getBitmap(url) == null) {
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
        val preloadOffset = (indexesOnScreen.size * preload / 2).roundToInt()
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