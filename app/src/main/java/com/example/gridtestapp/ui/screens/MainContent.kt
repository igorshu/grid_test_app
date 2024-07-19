@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalAnimationSpecApi::class)

package com.example.gridtestapp.ui.screens

import android.os.Build
import android.util.Log
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat.getString
import com.example.gridtestapp.R
import com.example.gridtestapp.logic.events.ChangeTheme
import com.example.gridtestapp.logic.events.ChangeVisibleRange
import com.example.gridtestapp.logic.events.ImagePressed
import com.example.gridtestapp.logic.events.ImagePressedNavigate
import com.example.gridtestapp.logic.events.LoadImageAgain
import com.example.gridtestapp.logic.events.UpdateImageWidthEvent
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.logic.viewmodels.ImageWidth
import com.example.gridtestapp.logic.viewmodels.MainViewModel
import com.example.gridtestapp.ui.composables.FailBox
import com.example.gridtestapp.ui.composables.ImageFailDialog
import com.example.gridtestapp.ui.composables.ImageLoader
import com.example.gridtestapp.ui.composables.Loader
import com.example.gridtestapp.ui.other.animationDuration
import com.example.gridtestapp.ui.other.easing
import com.example.gridtestapp.ui.other.mapState
import com.example.gridtestapp.ui.theme.DarkColorScheme
import com.example.gridtestapp.ui.theme.LightColorScheme
import com.robertlevonyan.compose.buttontogglegroup.RowToggleButtonGroup
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.get

/*
*
*   Главный экран
*
*/
@Composable
fun MainContent(
    paddingValues: PaddingValues,
    animatedScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
) {
    val appViewModel: AppViewModel = get()

    val coroutineScope = rememberCoroutineScope()
    val loading by appViewModel.state.mapState(coroutineScope) { it.loading }.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {

        ToggleButtons()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val mainViewModel: MainViewModel = get()
            val mainState by mainViewModel.state.collectAsState()

            if (loading) {
                Loader()
            } else {
                ImageGrid(animatedScope, sharedTransitionScope, mainState.widthConsumed)
            }
        }
    }
}

@Composable
fun ToggleButtons() {
    val appViewModel: AppViewModel = get()

    val primarySelection = remember {
        appViewModel.state.value.theme
    }

    val theme by appViewModel.themeFlow.collectAsState(initial = remember { appViewModel.state.value.theme })

    val context = LocalContext.current
    val buttonTexts = remember {
        val buttonCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { 3 } else { 2 }
        arrayOf(
            getString(context, R.string.by_default),
            getString(context, R.string.light),
            getString(context, R.string.dark),
        ).slice(3 - buttonCount..2).toTypedArray()
    }

    val isDark = theme.isDark(isSystemInDarkTheme())

    val selectedColor = (if (isDark) LightColorScheme else DarkColorScheme).background
    val unselectedColor = (if (isDark) DarkColorScheme else LightColorScheme).background
    val borderColor = (if (isDark) LightColorScheme else DarkColorScheme).background
    val selectedContentColor = (if (isDark) LightColorScheme else DarkColorScheme).inverseSurface
    val unselectedContentColor = (if (isDark) DarkColorScheme else LightColorScheme).inverseSurface

    RowToggleButtonGroup(
        modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
        buttonCount = buttonTexts.size,
        buttonTexts = buttonTexts,
        buttonHeight = 45.dp,
        primarySelection = primarySelection.ordinal,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
        borderColor = borderColor,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
    ) { index ->
        appViewModel.setEvent(ChangeTheme(index + if (buttonTexts.size == 3) 0 else 1))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ImageGrid(
    animatedScope: AnimatedContentScope,
    sharedTransitionScope: SharedTransitionScope,
    widthConsumed: Boolean,
) {
    val appViewModel: AppViewModel = get()
    val mainViewModel: MainViewModel = get()

    Log.d("cons", "widthConsumed = ${widthConsumed}")

    Box(
        modifier = Modifier
    ) {
        val gridState = rememberLazyGridState()

        val appState by appViewModel.state.collectAsState()

        val dpWidth = get<ImageWidth>().dpWidth

        val density = LocalDensity.current
        LaunchedEffect(gridState) {
            gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.let {
                mainViewModel.setEvent(UpdateImageWidthEvent(
                    width = it.size.width,
                    with(density) { it.size.width.toDp() - 4.dp }
                ))
            }
        }

        val selectedImage = appState.selectedImage

        val imageStates = appState.imageStates

        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(100.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {

            itemsIndexed(
                imageStates,
                key = { index, imageState -> "$index. ${imageState.url}" },
            ) { index, imageState ->

                val url = imageState.url
                val previewState = imageState.previewState
                Log.d("preview", "$index. $previewState")

                when (previewState) {
                    LoadState.LOADED -> {
                        val imageBitmap = imageState.previewBitmap
                        if (imageBitmap != null) {

                            val interactionSource = remember { MutableInteractionSource() }

                            with(sharedTransitionScope) {

                                val sharedContentState = rememberSharedContentState(key = index)

                                Image(
                                    painter = BitmapPainter(imageBitmap),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .zIndex(
                                            if (appState.currentImage?.index == index) {
                                                1F
                                            } else {
                                                0F
                                            }
                                        )
                                        .then(
                                            if (selectedImage?.index == index) {
                                                if (!selectedImage.consumed) {
                                                    appViewModel.setEvent(ImagePressedNavigate(url, index))
                                                }

                                                Modifier.sharedBounds(
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
                                        .then(
                                            if (dpWidth == 0.dp) {
                                                Modifier
                                                    .fillMaxSize()
                                                    .aspectRatio(1f)
                                            } else {
                                                Modifier
                                                    .width(dpWidth)
                                                    .height(dpWidth)
                                            }
                                        )
                                        .clickable(
                                            interactionSource,
                                            indication = null,
                                            onClick = {
                                                appViewModel.setEvent(ImagePressed(url, index))
                                            },
                                        )
                                )
                            }
                        }
                        else {
                            Box(modifier = Modifier
                                .aspectRatio(1.0f)
                                .background(Color.Cyan)) {}
                        }
                    }

                    LoadState.IDLE,
                    LoadState.LOADING -> {
                        ImageLoader(dpWidth)
                    }

                    LoadState.FAIL -> {
                        FailBox(url, dpWidth)
                    }
                }

                ImageFailDialog(
                    url,
                    onLoadAgain = { appViewModel.setEvent(LoadImageAgain(url, index)) }
                )
            }
        }

        LaunchedEffect(gridState) {
            snapshotFlow { gridState.layoutInfo.visibleItemsInfo }
                .filter { it.isNotEmpty() }
                .map { it.first().index..it.last().index }
                .distinctUntilChanged { old, new ->
                    old.first == new.first && old.last == new.last
                }
                .collect {
                    appViewModel.setEvent(ChangeVisibleRange(it))
                }
        }
    }
}
