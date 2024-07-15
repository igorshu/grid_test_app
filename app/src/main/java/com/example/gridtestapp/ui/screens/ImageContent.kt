@file:OptIn(ExperimentalAnimationSpecApi::class)

package com.example.gridtestapp.ui.screens

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import com.example.gridtestapp.core.cache.MemoryManager
import com.example.gridtestapp.logic.events.GoBackFromImage
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
import com.example.gridtestapp.ui.other.Hero
import com.example.gridtestapp.ui.other.animationDuration
import com.example.gridtestapp.ui.other.easing
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class,
    ExperimentalAnimationSpecApi::class
)
@Composable
fun ImageContent(
    index: Int,
    url: String,
    urls: List<String>,
    appViewModel: AppViewModel = get(),
    imageViewModel: ImageViewModel = koinViewModel(parameters = { parametersOf(urls, index, url) }),
    hero: Hero,
    paddingValues: PaddingValues,
) {

    val imageState = imageViewModel.state.collectAsState()
    val appState = appViewModel.state.collectAsState()

    with(hero.sharedTransitionScope) {

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

            val imagePadding = remember {
                paddingValues
            }

            HorizontalPager(state = pagerState) { index ->
                val url = urls[index]

                val urlState = imageState.value.originalUrlStates[url]

                val sharedContentState = rememberSharedContentState(key = "$index $url")

                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(imagePadding),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewUrlState = appState.value.previewUrlStates[url]
                        if (urlState == LOADED || previewUrlState == LOADED) {
                            val originalImage = MemoryManager.getOriginalBitmap(url)
                            val previewImage = MemoryManager.getPreviewBitmap(url)

                            val painter = if (originalImage != null) {
                                BitmapPainter(originalImage)
                            } else if (previewImage != null) {
                                LaunchedEffect(key1 = url) {
                                    imageViewModel.onEvent(LoadOriginalImageFromDisk(url, index))
                                }
                                BitmapPainter(previewImage)
                            } else null

                            if (painter != null) {
                                val interactionSource = remember { MutableInteractionSource() }
                                val zoomState = rememberZoomState(
                                    minScale = 0.011f,
                                    maxScale = 10f,
                                    exitScale = 0.6f,
                                    onExit = { zoomState ->
                                        appViewModel.onEvent(GoBackFromImage)
                                    }
                                )

                                val ratio = with(painter.intrinsicSize) { width / height }
                                val modifier = if (ratio > 1) {
                                    Modifier
                                        .aspectRatio(ratio)
                                        .fillMaxWidth()
                                } else {
                                    Modifier
                                        .aspectRatio(ratio)
                                        .fillMaxHeight()
                                }

                                Image(
                                    painter = painter,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = modifier
                                        .clickable(
                                            interactionSource,
                                            indication = null,
                                        ) {
                                            appViewModel.onEvent(ToggleFullScreen)
                                        }
                                        .zoomable(zoomState)
                                        .sharedBounds(
                                            sharedContentState,
                                            animatedVisibilityScope = hero.animatedScope,
                                            renderInOverlayDuringTransition = false,
                                            boundsTransform = { initialBounds, targetBounds ->
                                                keyframes {
                                                    durationMillis = animationDuration
                                                    initialBounds at 0 using easing(appState.value.currentScreen)
                                                    targetBounds at animationDuration
                                                }
                                            },
                                        ),
                                )
                            } else {
                                Box(modifier = Modifier.aspectRatio(1.0f)) {}
                            }
                        } else {
                            if (previewUrlState == FAIL || urlState == FAIL) {
                                FailBox(url, appViewModel = appViewModel)
                            } else {
                                Box(
                                    modifier = Modifier.aspectRatio(1.0f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.fillMaxSize(0.25f))
                                    LaunchedEffect(key1 = url) {
                                        imageViewModel.onEvent(
                                            LoadOriginalImageFromDisk(url, index)
                                        )
                                    }
                                }
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
        } else {
            Box(modifier = Modifier.fillMaxSize()) // чтобы не было странного поведения при удалении
        }
    }
}