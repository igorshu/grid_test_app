package com.example.gridtestapp.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.gridtestapp.logic.events.LoadImageEvent
import com.example.gridtestapp.logic.events.OnMainEvent
import com.example.gridtestapp.logic.events.ReloadImageEvent
import com.example.gridtestapp.logic.states.MainScreenState
import com.example.gridtestapp.ui.CELL_COUNT_LANDSCAPE
import com.example.gridtestapp.ui.CELL_COUNT_PORTRAIT
import com.example.gridtestapp.ui.cache.CacheManager.previewImageBitmap
import com.example.gridtestapp.ui.navigation.Routes
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.StateFlow
import org.koin.androidx.compose.get

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
            ) { url -> routes.navigate(ImageScreen(url)) }
        }
    }
}


@Composable
fun ImageGrid(state: MainScreenState, onEvent: OnMainEvent, toImageScreen: (url: String) -> Unit) {
    val orientation = LocalConfiguration.current.orientation
    val cellCount = if (orientation == Configuration.ORIENTATION_LANDSCAPE) CELL_COUNT_LANDSCAPE else CELL_COUNT_PORTRAIT

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Fixed(cellCount),
    ) {
        itemsIndexed(state.urls) {index, url ->
            if (state.loadedUrls.contains(url)) {
                val imageBitmap = remember(url) {
                    previewImageBitmap(url)
                }
                if (imageBitmap != null) {
                    Image(
                        painter = BitmapPainter(imageBitmap),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .aspectRatio(1.0f)
                            .clickable(onClick = {
                                toImageScreen(url)
                            })
                    )
                } else {
                    ImageLoader()
                    LaunchedEffect(url) {
                        onEvent(ReloadImageEvent(url))
                    }
                }
            } else {
                ImageLoader()
                LaunchedEffect(url) {
                    onEvent(LoadImageEvent(url))
                }
            }
        }
    }
}

@Composable
private fun ImageLoader() {
    Box(
        modifier = Modifier.aspectRatio(1.0f),
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
