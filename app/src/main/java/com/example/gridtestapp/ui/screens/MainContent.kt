package com.example.gridtestapp.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import com.example.gridtestapp.R
import com.example.gridtestapp.core.cache.MemoryManager
import com.example.gridtestapp.logic.events.ChangeTheme
import com.example.gridtestapp.logic.events.ChangeVisibleIndexes
import com.example.gridtestapp.logic.events.DismissImageFailDialog
import com.example.gridtestapp.logic.events.LoadImageAgain
import com.example.gridtestapp.logic.events.ShowImageFailDialog
import com.example.gridtestapp.logic.events.UpdateImageWidthEvent
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.logic.viewmodels.MainViewModel
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.theme.DarkColorScheme
import com.example.gridtestapp.ui.theme.LightColorScheme
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
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
) {
    val appState = appViewModel.state.collectAsState()

    val routes = get<Routes>()

    val systemUiController: SystemUiController = rememberSystemUiController()
    systemUiController.isSystemBarsVisible = true

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
                ImageGrid(appViewModel = appViewModel) { url, index ->
                    if (!routes.isImage()) {
                        routes.navigate(Routes.imageRoute(url, index))
                    }
                }
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


@Composable
fun ImageGrid(
    appViewModel: AppViewModel = get(),
    mainViewModel: MainViewModel = get(),
    toImageScreen: (url: String, index: Int,
) -> Unit) {

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
                        Image(
                            painter = BitmapPainter(imageBitmap),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1.0f)
                                .padding(2.dp)
                                .clickable(onClick = {
                                    toImageScreen(url, index)
                                })
                        )
                    } else {
                        Box(modifier = Modifier.aspectRatio(1.0f)) {}
                    }
                }
                LoadState.IDLE,
                LoadState.LOADING -> { ImageLoader() }
                LoadState.FAIL -> { FailBox(url, appViewModel = appViewModel) }
                else -> {}
            }

            ImageFailDialog(url, appViewModel = appViewModel)
        }
    }
}

@Composable
fun FailBox(
    url: String,
    appViewModel: AppViewModel = get()
) {
    Box(
        modifier = Modifier
            .aspectRatio(1.0f)
            .clickable { appViewModel.onEvent(ShowImageFailDialog(url)) },
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(id = R.string.error))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ImageFailDialog(
    url: String,
    appViewModel: AppViewModel = get()
) {
    val appState = appViewModel.state.collectAsState()

    if (appState.value.showImageFailDialog.isSome { it == url }) {
        val imageError = appState.value.imageErrors[url]
        imageError?.let {
            AlertDialog(onDismissRequest = { appViewModel.onEvent(DismissImageFailDialog) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(id = R.string.loading_error),
                            style = TextStyle(fontSize = 24.sp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(imageError.errorMessage)
                        Spacer(modifier = Modifier.height(5.dp))
                        if (imageError.canBeLoad) {
                            Button(
                                modifier = Modifier.padding(top = 15.dp),
                                onClick = { appViewModel.onEvent(LoadImageAgain(url)) }
                            ) {
                                Text(stringResource(id = R.string.load_again))
                            }
                        }
                        Button(
                            modifier = Modifier.padding(top = 15.dp),
                            onClick = { appViewModel.onEvent(DismissImageFailDialog) }
                        ) {
                            Text(stringResource(id = R.string.ok))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageLoader() {
    Box(
        modifier = Modifier
            .aspectRatio(1.0f),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.fillMaxSize(0.25f))
    }
}

@Composable
fun Loader() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.width(64.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
