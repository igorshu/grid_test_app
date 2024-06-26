package com.example.gridtestapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import com.example.gridtestapp.logic.events.LoadOriginalImageFromDisk
import com.example.gridtestapp.logic.events.OnAppEvent
import com.example.gridtestapp.logic.events.OnImageEvent
import com.example.gridtestapp.logic.events.ShowImageNotification
import com.example.gridtestapp.logic.events.ToggleFullScreen
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.states.ImageScreenState
import com.example.gridtestapp.logic.states.LoadState.FAIL
import com.example.gridtestapp.logic.states.LoadState.LOADED
import com.example.gridtestapp.logic.states.MainScreenState
import com.example.gridtestapp.ui.cache.MemoryManager
import com.example.gridtestapp.ui.navigation.Routes
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.collect
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/*
*
*   Экран с картинкой
*
*/
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageContent(
    imageState: ImageScreenState,
    appState: AppState,
    mainState: MainScreenState,
    onImageEvent: OnImageEvent,
    onAppEvent: OnAppEvent,
    routes: Routes,
    paddingValues: PaddingValues,
) {

    val left = remember { paddingValues.calculateLeftPadding(LayoutDirection.Ltr) }
    val top = remember { paddingValues.calculateTopPadding() }
    val right = remember { paddingValues.calculateRightPadding(LayoutDirection.Ltr) }
    val bottom = remember { paddingValues.calculateBottomPadding() }

    val systemUiController: SystemUiController = rememberSystemUiController()
    systemUiController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    systemUiController.isSystemBarsVisible = appState.showSystemBars

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = max(left - paddingValues.calculateLeftPadding(LayoutDirection.Ltr), 0.dp),
                top = max(top - paddingValues.calculateTopPadding(), 0.dp),
                end = max(right - paddingValues.calculateRightPadding(LayoutDirection.Ltr), 0.dp),
                bottom = max(bottom - paddingValues.calculateBottomPadding(), 0.dp),
            )
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val pagerState = rememberPagerState(
            initialPage = imageState.index,
            pageCount = { appState.urls.size }
        )

        LaunchedEffect(key1 = pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                val url = appState.urls[page]
                onImageEvent(ShowImageNotification(url))
            }
        }

        HorizontalPager(state = pagerState) { index ->
            val url = appState.urls[index]

            val urlState = imageState.originalUrlStates[url]
            if (urlState == LOADED) {
                val originalImage = remember {
                    MemoryManager.getOriginalBitmap(url)
                }
                val interactionSource = remember { MutableInteractionSource() }

                if (originalImage != null) {
                    val painter = BitmapPainter(originalImage)
                    val zoomState = rememberZoomState(
                        minScale = 0.011f,
                        maxScale = 10f,
                        exitScale = 0.6f,
                        onExit = routes::goBack,
                    )
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource,
                                indication = null,
                            ) {
                                onAppEvent(ToggleFullScreen)
                            }
                            .zoomable(zoomState)
                    )
                } else {
                    Box(modifier = Modifier.aspectRatio(1.0f)) {}
                }
            } else {
                val urlState = appState.previewUrlStates[url]
                if (urlState == FAIL) {
                    FailBox(onAppEvent, url)
                } else {
                    Box(
                        modifier = Modifier.aspectRatio(1.0f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.fillMaxSize(0.25f))
                        LaunchedEffect(key1 = url) {
                            onImageEvent(LoadOriginalImageFromDisk(url, index))
                        }
                    }
                }
            }
            ImageFailDialog(onAppEvent, appState, url)
        }
    }
}