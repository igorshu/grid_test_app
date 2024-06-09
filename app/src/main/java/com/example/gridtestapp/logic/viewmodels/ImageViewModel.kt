package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.gridtestapp.logic.events.ImageEvent
import com.example.gridtestapp.logic.events.ToggleBarsEvent
import com.example.gridtestapp.logic.states.ImageScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.dsl.module

/*
*
*   Вью модель для экрана с картинкой
*
*/
class ImageViewModel(private val application: Application): AndroidViewModel(application) {

    private val _state: MutableStateFlow<ImageScreenState> = MutableStateFlow(ImageScreenState(
        showTopBar = true,
        imageLoaded = false,
    ))
    val state: StateFlow<ImageScreenState> = _state.asStateFlow()

    fun onEvent(event: ImageEvent) {
        when (event) {
            is ToggleBarsEvent -> toggleBars()
        }
    }

    private fun toggleBars() {
        _state.update {
            it.copy(showTopBar = it.showTopBar.not())
        }
    }

    companion object {
        val module = module {
            single { ImageViewModel(get()) }
        }
    }
}