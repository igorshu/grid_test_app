@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalAnimationSpecApi::class)

package com.example.gridtestapp.ui.screens

import android.os.Build
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat.getString
import com.example.gridtestapp.R
import com.example.gridtestapp.core.cache.MemoryManager
import com.example.gridtestapp.logic.events.ChangeTheme
import com.example.gridtestapp.logic.events.ChangeVisibleIndexes
import com.example.gridtestapp.logic.events.ImagePressed
import com.example.gridtestapp.logic.events.LoadImageAgain
import com.example.gridtestapp.logic.events.UpdateImageWidthEvent
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.logic.viewmodels.MainViewModel
import com.example.gridtestapp.ui.composables.FailBox
import com.example.gridtestapp.ui.composables.ImageFailDialog
import com.example.gridtestapp.ui.composables.ImageLoader
import com.example.gridtestapp.ui.composables.Loader
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.other.Hero
import com.example.gridtestapp.ui.other.animationDuration
import com.example.gridtestapp.ui.other.easing
import com.example.gridtestapp.ui.theme.DarkColorScheme
import com.example.gridtestapp.ui.theme.LightColorScheme
import com.robertlevonyan.compose.buttontogglegroup.RowToggleButtonGroup
import org.koin.androidx.compose.get

/*
*
*   Главный экран
*
*/
@Composable
fun MainContent(
    paddingValues: PaddingValues,
    appViewModel: AppViewModel = get(),
    hero: Hero,
) {
    val appState = appViewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        val urls by remember {
            derivedStateOf {
                appState.value.urls
            }
        }

        ToggleButtons(appViewModel = appViewModel)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (urls.isEmpty()) {
                Loader()
            } else {
                val routes = get<Routes>()
                ImageGrid(
                    appViewModel = appViewModel,
                    hero = hero,
                )
            }
        }
    }
}

@Composable
fun ToggleButtons(
    appViewModel: AppViewModel = get()
) {
    val appState = appViewModel.state.collectAsState()

    val primarySelection = remember {
        appState.value.theme
    }
    val theme by remember {
        derivedStateOf {
            appState.value.theme
        }
    }

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
        appViewModel.onEvent(ChangeTheme(index + if (buttonTexts.size == 3) 0 else 1))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ImageGrid(
    appViewModel: AppViewModel = get(),
    mainViewModel: MainViewModel = get(),
    hero: Hero,
) {

    val appState = appViewModel.state.collectAsState()

    val indexesOnScreen = remember {
        hashSetOf<Int>()
    }

    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        gridState.layoutInfo.visibleItemsInfo.firstOrNull()?.let {
            mainViewModel.onEvent(UpdateImageWidthEvent(width = it.size.width))
        }
    }

    Box(
        modifier = Modifier
    ) {
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(100.dp),
        ) {
            itemsIndexed(
                appState.value.urls.toList(),
                key = { index, url -> "$index. $url" }
            ) { index, url ->

                LaunchedEffect(index) {
                    indexesOnScreen.add(index)
                    appViewModel.onEvent(ChangeVisibleIndexes(indexesOnScreen, index))
                }
                DisposableEffect(index) {
                    onDispose {
                        indexesOnScreen.remove(index)
                        appViewModel.onEvent(ChangeVisibleIndexes(indexesOnScreen, null))
                    }
                }

                when (appState.value.previewUrlStates[url]) {
                    LoadState.LOADED -> {
                        val imageBitmap = MemoryManager.getPreviewBitmap(url)
                        if (imageBitmap != null) {
                            with(hero.sharedTransitionScope) {
                                val sharedContentState = rememberSharedContentState(key = "$index $url")
                                val interactionSource = remember { MutableInteractionSource() }
                                Image(
                                    painter = BitmapPainter(imageBitmap),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .zIndex(if (appState.value.currentImage?.index == index) { 1F } else { 0F })
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

                                        )
                                        .aspectRatio(1.0f)
                                        .padding(2.dp)
                                        .clickable(
                                            interactionSource,
                                            indication = null,
                                            onClick = {
                                            appViewModel.onEvent(ImagePressed(url, index))
                                        })
                                )
                            }
                        } else {
                            Box(modifier = Modifier.aspectRatio(1.0f)) {}
                        }
                    }

                    LoadState.IDLE,
                    LoadState.LOADING -> {
                        ImageLoader()
                    }

                    LoadState.FAIL -> {
                        FailBox(url, appViewModel = appViewModel)
                    }

                    else -> {}
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
