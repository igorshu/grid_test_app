package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridtestapp.logic.events.ImageEvent
import com.example.gridtestapp.logic.states.ImageScreenState
import com.example.gridtestapp.ui.cache.CacheManager
import kotlinx.coroutines.Dispatchers
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
class ImageViewModel(private val application: Application, val url: String): AndroidViewModel(application) {

    private val _state: MutableStateFlow<ImageScreenState> = MutableStateFlow(ImageScreenState(
        imageLoaded = false,
        null,
    ))
    val state: StateFlow<ImageScreenState> = _state.asStateFlow()

    init {
        loadOriginalImage(url)
    }

    fun onEvent(event: ImageEvent) { }

    private fun loadOriginalImage(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = CacheManager.originalImageBitmap(url)
            _state.update {
                it.copy(image = bitmap)
            }
        }
    }
}