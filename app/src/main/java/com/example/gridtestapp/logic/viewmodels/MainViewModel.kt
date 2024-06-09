package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridtestapp.logic.events.LoadImageEvent
import com.example.gridtestapp.logic.events.MainEvent
import com.example.gridtestapp.logic.events.ReloadImageEvent
import com.example.gridtestapp.logic.states.MainScreenState
import com.example.gridtestapp.ui.cache.CacheManager
import com.example.gridtestapp.ui.exceptions.ImageLoadException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.dsl.module

/*
*
*   Вью модель для главоного экрана с превьюшками
*
*/
class MainViewModel(private val application: Application): AndroidViewModel(application) {

    private val handler = CoroutineExceptionHandler { _, exception -> showError(exception)}
    private val imageExceptionHandler = CoroutineExceptionHandler { _, exception -> loadImageError(exception)}

    private fun loadImageError(throwable: Throwable) {
        if (throwable is ImageLoadException) {
            CacheManager.removeBothImages(throwable.url)

            _state.update {
                val url = throwable.url
                val urls = it.urls.toMutableList().apply { remove(url) }
                it.copy(urls = urls.toList())
            }
        } else {
            Log.e("Error", throwable.message.toString())
        }
    }

    private val _state: MutableStateFlow<MainScreenState> = MutableStateFlow(MainScreenState(listOf(), hashSetOf()))
    val state: StateFlow<MainScreenState> = _state.asStateFlow()


    private fun showError(throwable: Throwable) {
        viewModelScope.launch {
            Toast.makeText(application, throwable.message, Toast.LENGTH_LONG).show()
        }
    }

    init {
        CacheManager.init(application)

        viewModelScope.launch(handler + Dispatchers.IO) {
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
        lines.forEach { url ->
            if (CacheManager.isCached(url)) {
                _state.value.loadedUrls.add(url)
            }
        }

        _state.update { it.copy(urls = lines, loadedUrls = _state.value.loadedUrls.toHashSet()) }
    }

    private fun loadImage(url: String, removeBefore: Boolean) {
        viewModelScope.launch(imageExceptionHandler + Dispatchers.IO) {
            if (removeBefore) {
                CacheManager.removeBothImages(url)
            }

            if (CacheManager.isNotCached(url)) {
                if (CacheManager.loadImage(url)) {
                    _state.update {
                        val loadedUrls = it.loadedUrls.toHashSet().apply { add(url) }
                        it.copy(loadedUrls = loadedUrls)
                    }
                }
            } else {
                _state.update {
                    val loadedUrls = it.loadedUrls.toHashSet().apply { add(url) }
                    it.copy(loadedUrls = loadedUrls)
                }
            }
        }
    }

    fun onEvent(event: MainEvent) {
        when (event) {
            is LoadImageEvent -> loadImage(event.url, false)
            is ReloadImageEvent -> loadImage(event.url, true)
        }
    }

    companion object {
        val module = module {
            single { MainViewModel(get()) }
        }
    }
}