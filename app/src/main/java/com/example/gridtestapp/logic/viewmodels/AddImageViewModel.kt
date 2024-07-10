package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridtestapp.core.cache.CacheManager
import com.example.gridtestapp.core.cache.ImageLoader
import com.example.gridtestapp.core.cache.MemoryManager
import com.example.gridtestapp.core.connection.ConnectionManager
import com.example.gridtestapp.logic.coroutines.ImageLoadFail
import com.example.gridtestapp.logic.coroutines.UnknownFail
import com.example.gridtestapp.logic.coroutines.imageExceptionHandler
import com.example.gridtestapp.logic.coroutines.showError
import com.example.gridtestapp.logic.events.AddImageEvent
import com.example.gridtestapp.logic.events.AppEvent
import com.example.gridtestapp.logic.events.CancelAdd
import com.example.gridtestapp.logic.events.LoadImage
import com.example.gridtestapp.logic.events.LoadImageAgain
import com.example.gridtestapp.logic.states.AddImageState
import com.example.gridtestapp.logic.states.ImageError
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.ui.navigation.Routes
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

class AddImageViewModel(application: Application, val url: String): AndroidViewModel(application), KoinComponent {

    private val loader: ImageLoader by inject()
    private val routes: Routes by inject()
    private val connectionManager: ConnectionManager by inject()

    private val handler = CoroutineExceptionHandler { _, exception ->
        showError(
            application,
            viewModelScope,
            exception
        )
    }

    private val imageLoadFail: ImageLoadFail = { url, errorMessage, canBeLoad ->
        _state.update {
            it.copy(
                loadState = LoadState.FAIL,
                imageError = ImageError(errorMessage, canBeLoad),
            )
        }
    }

    private val unknownFail: UnknownFail = { throwable ->
        viewModelScope.launch(handler) {
            Toast.makeText(application, throwable.message.toString(), Toast.LENGTH_LONG).show()
        }
    }

    private val _imageExceptionHandler = imageExceptionHandler(imageLoadFail, unknownFail, connectionManager)

    private val _state: MutableStateFlow<AddImageState> = MutableStateFlow(AddImageState.init())
    val state: StateFlow<AddImageState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            connectionManager.listen { restoreAfterDisconnect() }
        }
    }

    private fun restoreAfterDisconnect() {
        loadImage(url)
    }

    fun onAppEvent(event: AppEvent) {
        if (event is LoadImageAgain) {
            loadImage(event.url)
        }
    }

    fun onEvent(event: AddImageEvent) {
        when (event) {
            is LoadImage -> loadImage(event.url)
            is CancelAdd -> cancel()
        }
    }

    private fun cancel() {
        routes.goBack()
    }

    private fun loadImage(url: String) {
        viewModelScope.launch(_imageExceptionHandler + Dispatchers.IO) {
            loader.loadImage(
                url,
                onLoading = {
                    _state.update { it.copy(loadState = LoadState.LOADING) }
                },
                onLoaded = {
                    val bitmap: ImageBitmap? = CacheManager.originalImageBitmap(url)
                    if (bitmap != null) {
                        MemoryManager.addOriginalBitmap(url, bitmap)
                        _state.update { it.copy(loadState = LoadState.LOADED) }
                    }
                }
            )
        }
    }

    companion object {
        val module = module {
            viewModel { params -> AddImageViewModel(get(), url = params.get()) }
        }
    }

    override fun onCleared() {
        Log.d("addImageViewModel", "onCleared")
        super.onCleared()
    }
}