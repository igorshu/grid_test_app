package com.example.gridtestapp.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.example.gridtestapp.core.cache.MemoryManager
import com.example.gridtestapp.logic.events.LoadImageAgain
import com.example.gridtestapp.logic.events.LoadOriginalImageFromDisk
import com.example.gridtestapp.logic.events.ShowImageNotification
import com.example.gridtestapp.logic.events.ToggleFullScreen
import com.example.gridtestapp.logic.events.UpdateCurrentImage
import com.example.gridtestapp.logic.states.LoadState.FAIL
import com.example.gridtestapp.logic.states.LoadState.LOADED
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.logic.viewmodels.ImageViewModel
import com.example.gridtestapp.ui.composables.FailBox
import com.example.gridtestapp.ui.composables.ImageFailDialog
import com.example.gridtestapp.ui.navigation.Routes
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/*
*
*   Экран с картинкой
*
*/
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageContent(
    index: Int,
    url: String,
    urls: List<String>,
    appViewModel: AppViewModel = get(),
    imageViewModel: ImageViewModel = koinViewModel(parameters = { parametersOf(urls, index, url) })
) {

    val imageState = imageViewModel.state.collectAsState()
    val appState = appViewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {

        if (!appState.value.hideImage) {
            val pagerState = rememberPagerState(
                initialPage = imageState.value.index,
                pageCount = { urls.size }
            )

            LaunchedEffect(key1 = pagerState.currentPage) {
                val currentPage = pagerState.currentPage
                val currentUrl = urls[currentPage]

                imageViewModel.onEvent(ShowImageNotification(currentUrl))
                appViewModel.onEvent(UpdateCurrentImage(currentUrl, currentPage))
            }

            HorizontalPager(state = pagerState) { index ->
                val url = urls[index]

                val urlState = imageState.value.originalUrlStates[url]

                if (urlState == LOADED) {
                    val originalImage = remember {
                        MemoryManager.getOriginalBitmap(url)
                    }
                    val interactionSource = remember { MutableInteractionSource() }

                    if (originalImage != null) {
                        val painter = BitmapPainter(originalImage)
                        val routes = get<Routes>()
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
                                    appViewModel.onEvent(ToggleFullScreen)
                                }
                                .zoomable(zoomState)
                        )
                    } else {
                        Box(modifier = Modifier.aspectRatio(1.0f)) {}
                    }
                } else {
                    val urlState = appState.value.previewUrlStates[url]
                    if (urlState == FAIL) {
                        FailBox(url, appViewModel = appViewModel)
                    } else {
                        Box(
                            modifier = Modifier.aspectRatio(1.0f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.fillMaxSize(0.25f))
                            LaunchedEffect(key1 = url) {
                                imageViewModel.onEvent(LoadOriginalImageFromDisk(url, index))
                            }
                        }
                    }
                }
                ImageFailDialog(
                    appState.value.showImageFailDialog.isSome { it == url },
                    appState.value.imageErrors[url],
                    appViewModel = appViewModel,
                    onLoadAgain = { appViewModel.onEvent(LoadImageAgain(url)) }
                )
            }
        }
    }
}