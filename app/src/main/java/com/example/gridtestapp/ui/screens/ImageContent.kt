@file:OptIn(ExperimentalAnimationSpecApi::class)

package com.example.gridtestapp.ui.screens

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import com.example.gridtestapp.logic.events.DisableSharedAnimation
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
import com.example.gridtestapp.logic.viewmodels.ImageWidth
import com.example.gridtestapp.ui.composables.FailBox
import com.example.gridtestapp.ui.composables.ImageFailDialog
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
@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ImageContent(
    index: Int,
    url: String,
    urls: List<String>,
    animatedScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
    paddingValues: PaddingValues,
) {

    val appViewModel: AppViewModel = get()
    val imageViewModel: ImageViewModel = koinViewModel(parameters = { parametersOf(urls, index, url) })

    val imageState = imageViewModel.state.collectAsState().value
    val appState = appViewModel.state.collectAsState().value

    with(sharedTransitionScope) {

        val pagerState = rememberPagerState(
            initialPage = imageState.index,
            pageCount = { urls.size }
        )

        LaunchedEffect(key1 = pagerState.currentPage) {
            val currentPage = pagerState.currentPage
            val currentUrl = urls[currentPage]

            imageViewModel.onEvent(ShowImageNotification(currentUrl))
            appViewModel.setEvent(UpdateCurrentImage(currentUrl, currentPage))
        }

        val imagePadding = remember {
            paddingValues
        }

        HorizontalPager(state = pagerState) { index ->
            val url = urls[index]

            val sharedContentState = rememberSharedContentState(key = index)

            val urlState = imageState.originalUrlStates[url]
            val itemImageState = remember { appViewModel.imageStates[index].value }

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
                    val previewUrlState = itemImageState.previewState
                    if (urlState == LOADED || previewUrlState == LOADED) {
                        val originalImage = MemoryManager.getOriginalBitmap(url)
                        val previewImage = itemImageState.previewBitmap

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
                                onExit = { _ ->
                                    appViewModel.setEvent(DisableSharedAnimation)
                                    appViewModel.setEvent(GoBackFromImage)
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
                                        appViewModel.setEvent(ToggleFullScreen)
                                    }
                                    .then(
                                        if (appState.sharedAnimation) {
                                            Modifier
                                                .sharedBounds(
                                                    sharedContentState,
                                                    animatedVisibilityScope = animatedScope,
                                                    renderInOverlayDuringTransition = false,
                                                    boundsTransform = { initialBounds, targetBounds ->
                                                        keyframes {
                                                            durationMillis = animationDuration
                                                            initialBounds at 0 using easing
                                                            targetBounds at animationDuration
                                                        }
                                                    },
                                                )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .zoomable(zoomState),
                            )
                        } else {
                            Box(modifier = Modifier.aspectRatio(1.0f)) {}
                        }
                    } else {
                        if (previewUrlState == FAIL || urlState == FAIL) {
                            FailBox(url, get<ImageWidth>().dpWidth)
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
            ImageFailDialog { errorUrl, errorIndex -> appViewModel.setEvent(LoadImageAgain(errorUrl, errorIndex)) }
        }
    }
}