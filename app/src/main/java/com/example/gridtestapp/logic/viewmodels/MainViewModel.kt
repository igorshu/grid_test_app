package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridtestapp.logic.events.MainEvent
import com.example.gridtestapp.logic.events.OnMainEvent
import com.example.gridtestapp.logic.events.UpdateImageWidthEvent
import com.example.gridtestapp.logic.states.MainScreenState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.dsl.module

/*
*
*   Вью модель для главного экрана с превьюшками
*
*/
class MainViewModel(private val application: Application): AndroidViewModel(application), KoinComponent {

    private val _state: MutableStateFlow<MainScreenState> = MutableStateFlow(MainScreenState())
    val state: StateFlow<MainScreenState> = _state.asStateFlow()

    private val _event: MutableSharedFlow<MainEvent> = MutableSharedFlow()

    init {
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        viewModelScope.launch {
            _event.collect {
                onEvent(it)
            }
        }
    }

    fun setEvent(event: MainEvent) {
        viewModelScope.launch { _event.emit(event) }
    }

    private val onEvent: OnMainEvent = { event ->
        when (event) {
            is UpdateImageWidthEvent -> changeWidth(event.width, event.dpWidth)
        }
    }

    private fun changeWidth(width: Int, dpWidth: Dp) {
        _state.update { it.copy(widthConsumed = true) }
        get<ImageWidth>().pxWidth = width
        get<ImageWidth>().dpWidth = dpWidth
    }

    companion object {
        val module = module {
            single { MainViewModel(get()) }
            single { ImageWidth(0, 0.dp) }
        }
    }
}