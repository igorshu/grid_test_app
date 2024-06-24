package com.example.gridtestapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import com.example.gridtestapp.logic.events.OnImageEvent
import com.example.gridtestapp.logic.events.OnTopBarEvent
import com.example.gridtestapp.logic.events.ToggleBars
import com.example.gridtestapp.logic.events.ToggleTopBar
import com.example.gridtestapp.logic.states.ImageScreenState
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
    imageState: StateFlow<ImageScreenState>,
    onEvent: OnImageEvent,
    onTopBarEvent: OnTopBarEvent,
    paddingValues: PaddingValues,
) {

    val state = imageState.collectAsState()
    val top = remember {
        paddingValues.calculateTopPadding()
    }

    val systemUiController: SystemUiController = rememberSystemUiController()
    systemUiController.isSystemBarsVisible = state.value.showTopBar

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (paddingValues.calculateTopPadding() == 0.dp) top else 0.dp )
            .background(MaterialTheme.colorScheme.background)
    ) {

        if (state.value.image != null) {
            Image(
                painter = BitmapPainter(state.value.image!!),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        onEvent(ToggleBars)
                        onTopBarEvent(ToggleTopBar)
                    }
                    .zoomable(rememberZoomState())
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