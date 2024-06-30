package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.gridtestapp.logic.events.OnMainEvent
import com.example.gridtestapp.logic.events.UpdateImageWidthEvent
import com.example.gridtestapp.logic.states.MainScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.dsl.module

/*
*
*   Вью модель для главного экрана с превьюшками
*
*/
class MainViewModel(private val application: Application): AndroidViewModel(application), KoinComponent {

    private val _state: MutableStateFlow<MainScreenState> = MutableStateFlow(MainScreenState.init())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    val onEvent: OnMainEvent = { event ->
        when (event) {
            is UpdateImageWidthEvent -> changeWidth(event.width)
        }
    }

    private fun changeWidth(width: Int) {
        _state.update { it.copy(widthConsumed = true) }
        get<ImageWidth>().value = width
    }

    companion object {
        val module = module {
            single { MainViewModel(get()) }
            single { ImageWidth(1) }
        }
    }
}