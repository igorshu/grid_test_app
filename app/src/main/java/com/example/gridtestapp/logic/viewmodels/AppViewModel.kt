package com.example.gridtestapp.logic.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gridtestapp.core.ConnectionManager
import com.example.gridtestapp.logic.coroutines.showError
import com.example.gridtestapp.logic.events.AppEvent
import com.example.gridtestapp.logic.events.ImageScreenEvent
import com.example.gridtestapp.logic.events.MainScreenEvent
import com.example.gridtestapp.logic.events.SharePressed
import com.example.gridtestapp.logic.events.ToggleFullScreen
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.states.Screen
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.dsl.module

class AppViewModel(private val application: Application): AndroidViewModel(application), KoinComponent {

    private val handler = CoroutineExceptionHandler { _, exception -> showError(application, viewModelScope, exception) }

    private val appName = application.getString(application.applicationInfo.labelRes)

    val _state: MutableStateFlow<AppState> = MutableStateFlow(AppState.init(appName))
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch(handler + Dispatchers.IO) {
            ConnectionManager.init(application)
        }
    }

    fun onEvent(event: AppEvent) {
        when (event) {
            is ToggleFullScreen -> _state.update {
                if (it.currentScreen == Screen.IMAGE) { it.copy(showTopBar = !it.showTopBar, showSystemBars = !it.showSystemBars) } else { it }
            }
            is MainScreenEvent -> _state.update {
                it.copy(
                    showTopBar = true,
                    showSystemBars = true,
                    title = appName,
                    showBack = false,
                    currentScreen = Screen.MAIN)
            }
            is ImageScreenEvent -> _state.update {
                it.copy(showTopBar = true,
                    showSystemBars = true,
                    title = event.url,
                    shareUrl = event.url,
                    showBack = true,
                    currentScreen = Screen.IMAGE)
            }
            is SharePressed -> shareUrl()
        }
    }

    private fun shareUrl() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_TEXT, state.value.shareUrl)
        shareIntent.setType("text/plain")
        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        application.startActivity(shareIntent)
    }

    companion object {
        val module = module {
            single { AppViewModel(get()) }
        }
    }

}