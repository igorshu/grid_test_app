package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.gridtestapp.logic.events.ImageTopBar
import com.example.gridtestapp.logic.events.MainTopBar
import com.example.gridtestapp.logic.events.ToggleTopBar
import com.example.gridtestapp.logic.events.TopBarEvent
import com.example.gridtestapp.logic.states.TopBarState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.dsl.module

class TopBarViewModel(private val application: Application): AndroidViewModel(application),
    KoinComponent {

    private val appName = application.getString(application.applicationInfo.labelRes)

    private val _state: MutableStateFlow<TopBarState> = MutableStateFlow(TopBarState(showBack = false, showTopBar = true, title = appName))
    val state: StateFlow<TopBarState> = _state.asStateFlow()

    fun onEvent(event: TopBarEvent) {
        when (event) {
            is ToggleTopBar -> _state.update { it.copy(showTopBar = !it.showTopBar) }
            is MainTopBar -> _state.update { it.copy(showTopBar = true, title = event.url, showBack = true) }
            is ImageTopBar -> _state.update { it.copy(showTopBar = true, title = appName, showBack = false) }
        }
    }

    companion object {
        val module = module {
            single { TopBarViewModel(get()) }
        }
    }

}