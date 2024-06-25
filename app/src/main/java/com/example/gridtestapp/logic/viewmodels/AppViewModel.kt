package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.gridtestapp.logic.events.ImageScreenEvent
import com.example.gridtestapp.logic.events.MainScreenEvent
import com.example.gridtestapp.logic.events.ToggleSystemBars
import com.example.gridtestapp.logic.events.ToggleTopBar
import com.example.gridtestapp.logic.events.AppEvent
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.states.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.dsl.module

class AppViewModel(private val application: Application): AndroidViewModel(application),
    KoinComponent {

    private val appName = application.getString(application.applicationInfo.labelRes)

    private val _state: MutableStateFlow<AppState> = MutableStateFlow(AppState(
        showBack = false,
        showTopBar = true,
        showSystemBars = true,
        title = appName,
        currentScreen = Screen.MAIN
        ))
    val state: StateFlow<AppState> = _state.asStateFlow()

    fun onEvent(event: AppEvent) {
        when (event) {
            is ToggleTopBar -> _state.update {
                if (it.currentScreen == Screen.IMAGE) { it.copy(showTopBar = !it.showTopBar) } else { it }
            }
            is ToggleSystemBars -> _state.update {
                if (it.currentScreen == Screen.IMAGE) { it.copy(showSystemBars = !it.showSystemBars) } else { it }
            }
            is MainScreenEvent -> _state.update { it.copy(showTopBar = true, title = appName, showBack = false, currentScreen = Screen.MAIN) }
            is ImageScreenEvent -> _state.update { it.copy(showTopBar = true, title = event.url, showBack = true, currentScreen = Screen.IMAGE) }
        }
    }

    companion object {
        val module = module {
            single { AppViewModel(get()) }
        }
    }

}