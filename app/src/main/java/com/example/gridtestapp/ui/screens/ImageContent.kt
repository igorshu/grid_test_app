package com.example.gridtestapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.example.gridtestapp.logic.events.OnImageEvent
import com.example.gridtestapp.logic.events.ToggleBarsEvent
import com.example.gridtestapp.logic.states.ImageScreenState
import com.example.gridtestapp.ui.cache.CacheManager
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
fun ImageContent(url: String, imageState: StateFlow<ImageScreenState>, onEvent: OnImageEvent) {

    val state = imageState.collectAsState()

    val systemUiController: SystemUiController = rememberSystemUiController()
    systemUiController.isSystemBarsVisible = state.value.showTopBar

    val imageBitmap = remember(url) {
        CacheManager.originalImageBitmap(url)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = BitmapPainter(imageBitmap),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clickable(onClick = {
                onEvent(ToggleBarsEvent)
            }).zoomable(rememberZoomState())
        )
    }
}