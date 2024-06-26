package com.example.gridtestapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import com.example.gridtestapp.logic.events.OnImageEvent
import com.example.gridtestapp.logic.events.OnAppBarEvent
import com.example.gridtestapp.logic.events.ToggleSystemBars
import com.example.gridtestapp.logic.events.ToggleTopBar
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.states.ImageScreenState
import com.example.gridtestapp.ui.navigation.Routes
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.StateFlow
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/*
*
*   Экран с картинкой
*
*/
@Composable
fun ImageContent(
    imageStateFlow: StateFlow<ImageScreenState>,
    appStateFlow: StateFlow<AppState>,
    onEvent: OnImageEvent,
    onAppBarEvent: OnAppBarEvent,
    routes: Routes,
    paddingValues: PaddingValues,
) {

    val imageState = imageStateFlow.collectAsState()
    val appState = appStateFlow.collectAsState()
    val top = remember {
        paddingValues.calculateTopPadding()
    }
    val bottom = remember {
        paddingValues.calculateBottomPadding()
    }

    val systemUiController: SystemUiController = rememberSystemUiController()
    systemUiController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    systemUiController.isSystemBarsVisible = appState.value.showSystemBars

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = top - paddingValues.calculateTopPadding(),
                bottom = bottom - paddingValues.calculateBottomPadding(),
            )
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (imageState.value.image != null) {
            val interactionSource = remember { MutableInteractionSource() }

            val painter = BitmapPainter(imageState.value.image!!)
            val zoomState = rememberZoomState(
                minScale = 0.011f,
                maxScale = 10f,
                exitScale = 0.6f,
                onExit = routes::goBack
            )
            println("zoomState.scale = ${zoomState.scale}")

            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource,
                        indication = null,
                    ) {
                        onAppBarEvent(ToggleTopBar)
                        onAppBarEvent(ToggleSystemBars)
                    }
                    .zoomable(zoomState)
            )
        } else {
            Box(
                modifier = Modifier.aspectRatio(1.0f)
            ) {
                CircularProgressIndicator(modifier = Modifier.fillMaxSize(0.25f))
            }
        }
    }
}