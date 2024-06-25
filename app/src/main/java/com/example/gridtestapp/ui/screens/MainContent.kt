package com.example.gridtestapp.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.gridtestapp.logic.events.ChangeVisibleIndexes
import com.example.gridtestapp.logic.events.OnMainEvent
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.states.MainScreenState
import com.example.gridtestapp.ui.cache.MemoryManager
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.other.onWidthChanged
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.get
import java.net.URLEncoder

/*
*
*   Главный экран
*
*/
@Composable
fun MainContent(mainState: StateFlow<MainScreenState>, onEvent: OnMainEvent) {

    val state = mainState.collectAsState()
    val routes = get<Routes>()

    val systemUiController: SystemUiController = rememberSystemUiController()
    systemUiController.isSystemBarsVisible = true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.value.urls.isEmpty()) {
            Loader()
        } else {
            ImageGrid(state.value,
                onEvent = onEvent
            ) { url -> routes.navigate(Routes.IMAGE.replace("{url}", URLEncoder.encode(url, "UTF-8"))) }
        }
    }
}


@Composable
fun ImageGrid(state: MainScreenState, onEvent: OnMainEvent, toImageScreen: (url: String) -> Unit) {

    val indexesOnScreen = remember {
        hashSetOf<Int>()
    }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(100.dp),
    ) {
        itemsIndexed(state.urls.toList(),
            key = { index, url -> url }
        ) {index, url ->

            LaunchedEffect(index) {
                indexesOnScreen.add(index)
                onEvent(ChangeVisibleIndexes(indexesOnScreen, index))
            }
            DisposableEffect(index) {
                onDispose {
                    indexesOnScreen.remove(index)
                    onEvent(ChangeVisibleIndexes(indexesOnScreen, null))
                }
            }

            when (state.urlStates[url]) {
                LoadState.LOADED -> {
                    val imageBitmap = MemoryManager.getBitmap(url)
                    if (imageBitmap != null) {
                        Image(
                            painter = BitmapPainter(imageBitmap),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .aspectRatio(1.0f)
                                .padding(2.dp)
                                .clickable(onClick = {
                                    toImageScreen(url)
                                })
                                .onWidthChanged(state, onEvent),
                        )
                    } else {
                        Box(modifier = Modifier.aspectRatio(1.0f)) {}
                    }
                }
                LoadState.LOADING -> {
                    ImageLoader(state, onEvent)
                }
                LoadState.FAIL -> {
                    Box(
                        modifier = Modifier.aspectRatio(1.0f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Ошибка")
                    }
                }
                LoadState.IDLE -> {
                    ImageLoader(state, onEvent)
                }
                else -> {}
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
