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
import com.example.gridtestapp.logic.events.ChangeVisibleIndexes
import com.example.gridtestapp.logic.events.DismissImageFailDialog
import com.example.gridtestapp.logic.events.LoadImageAgain
import com.example.gridtestapp.logic.events.OnAppEvent
import com.example.gridtestapp.logic.events.OnMainEvent
import com.example.gridtestapp.logic.events.ShowImageFailDialog
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.states.MainScreenState
import com.example.gridtestapp.core.cache.MemoryManager
import com.example.gridtestapp.logic.events.ChangeTheme
import com.example.gridtestapp.logic.states.Theme
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.other.onWidthChanged
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
    mainState: MainScreenState,
    appState: AppState,
    onMainEvent: OnMainEvent,
    onAppEvent: OnAppEvent,
    paddingValues: PaddingValues,
) {

    val routes = get<Routes>()

    val systemUiController: SystemUiController = rememberSystemUiController()
    systemUiController.isSystemBarsVisible = true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        val theme = appState.theme
        ToggleButtons(theme, onAppEvent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (appState.urls.isEmpty()) {
                Loader()
            } else {
                ImageGrid(
                    mainState = mainState,
                    appState = appState,
                    onMainEvent = onMainEvent,
                    onAppEvent = onAppEvent,
                ) { url, index ->
                    if (!routes.isImage()) {
                        routes.navigate(Routes.imageRoute(url, index))
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleButtons(theme: Theme, onAppEvent: OnAppEvent) {
    val primarySelection = remember {
        theme
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

    val selectedColor = (if (theme.isDark(isSystemInDarkTheme())) LightColorScheme else DarkColorScheme).background
    val unselectedColor = (if (theme.isDark(isSystemInDarkTheme())) DarkColorScheme else LightColorScheme).background
    val borderColor = (if (theme.isDark(isSystemInDarkTheme())) LightColorScheme else DarkColorScheme).background
    val selectedContentColor = (if (theme.isDark(isSystemInDarkTheme())) LightColorScheme else DarkColorScheme).inverseSurface
    val unselectedContentColor = (if (theme.isDark(isSystemInDarkTheme())) DarkColorScheme else LightColorScheme).inverseSurface

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
        onAppEvent(ChangeTheme(index + if (buttonTexts.size == 3) 0 else 1))
    }
}


@Composable
fun ImageGrid(mainState: MainScreenState,
              appState: AppState,
              onMainEvent: OnMainEvent,
              onAppEvent: OnAppEvent,
              toImageScreen: (url: String, index: Int) -> Unit) {

    val indexesOnScreen = remember {
        hashSetOf<Int>()
    }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(100.dp),
    ) {
        itemsIndexed(appState.urls.toList(),
            key = { _, url -> url }
        ) { index, url ->

            LaunchedEffect(index) {
                indexesOnScreen.add(index)
                onAppEvent(ChangeVisibleIndexes(indexesOnScreen, index))
            }
            DisposableEffect(index) {
                onDispose {
                    indexesOnScreen.remove(index)
                    onAppEvent(ChangeVisibleIndexes(indexesOnScreen, null))
                }
            }

            when (appState.previewUrlStates[url]) {
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
                                .onWidthChanged(mainState, onMainEvent),
                        )
                    } else {
                        Box(modifier = Modifier.aspectRatio(1.0f)) {}
                    }
                }
                LoadState.IDLE,
                LoadState.LOADING -> {
                    ImageLoader(mainState, onMainEvent)
                }
                LoadState.FAIL -> {
                    FailBox(onAppEvent, url)
                }
                else -> {}
            }

            ImageFailDialog(onAppEvent, appState, url)
        }
    }
}

@Composable
fun FailBox(
    onAppEvent: OnAppEvent,
    url: String
) {
    Box(
        modifier = Modifier
            .aspectRatio(1.0f)
            .clickable { onAppEvent(ShowImageFailDialog(url)) },
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(id = R.string.error))
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ImageFailDialog(
    onAppEvent: OnAppEvent,
    appState: AppState,
    url: String
) {
    if (appState.showImageFailDialog.isSome { it == url }) {
        val imageError = appState.imageErrors[url]
        imageError?.let {
            AlertDialog(onDismissRequest = { onAppEvent(DismissImageFailDialog) }) {
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
                                onClick = { onAppEvent(LoadImageAgain(url)) }
                            ) {
                                Text(stringResource(id = R.string.load_again))
                            }
                        }
                        Button(
                            modifier = Modifier.padding(top = 15.dp),
                            onClick = { onAppEvent(DismissImageFailDialog) }
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
fun ImageLoader(state: MainScreenState, onEvent: OnMainEvent) {
    Box(
        modifier = Modifier
            .aspectRatio(1.0f)
            .onWidthChanged(state, onEvent),
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
