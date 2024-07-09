package com.example.gridtestapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gridtestapp.R
import com.example.gridtestapp.core.cache.MemoryManager
import com.example.gridtestapp.logic.events.AddImage
import com.example.gridtestapp.logic.events.CancelAdd
import com.example.gridtestapp.logic.events.LoadImage
import com.example.gridtestapp.logic.events.ToggleFullScreen
import com.example.gridtestapp.logic.states.LoadState
import com.example.gridtestapp.logic.states.LoadState.FAIL
import com.example.gridtestapp.logic.viewmodels.AddImageViewModel
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.koin.androidx.compose.get
import org.koin.core.parameter.parametersOf

@Composable
fun AddImageContent(
    url: String,
    addImageViewModel: AddImageViewModel = get(parameters = { parametersOf(url) }),
    appViewModel: AppViewModel = get(),
) {

    val state = addImageViewModel.state.collectAsState()

    if (state.value.loadState == LoadState.LOADED) {
        val originalImage = remember {
            MemoryManager.getOriginalBitmap(url)
        }
        if (originalImage != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val zoomState = rememberZoomState(
                minScale = 0.011f,
                maxScale = 10f,
                exitScale = 0.6f,
                onExit = { addImageViewModel.onEvent(CancelAdd) },
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 10.dp)
            ) {
                Image(
                    painter = BitmapPainter(originalImage),
                    contentDescription = "Add Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                        .clickable(
                            interactionSource,
                            indication = null,
                        ) {
                            appViewModel.onEvent(ToggleFullScreen)
                        }
                        .zoomable(zoomState)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Spacer(modifier = Modifier.weight(0.5f))
                    FilledTonalButton(onClick = {
                        appViewModel.onEvent(AddImage(url))
                    }) {
                        Text(stringResource(id = R.string.add))
                    }
                    Spacer(modifier = Modifier.weight(1.0f))
                    FilledTonalButton(onClick = {
                        addImageViewModel.onEvent(CancelAdd)
                    }) {
                        Text(stringResource(id = R.string.cancel))
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                }
            }
        } else {
            Box(modifier = Modifier.aspectRatio(1.0f)) {}
        }
    } else {
        val urlState = state.value.loadState
        if (urlState == FAIL) {
            FailBox(url, appViewModel = appViewModel)
        } else {
            Box(
                modifier = Modifier.aspectRatio(1.0f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.fillMaxSize(0.25f))
                LaunchedEffect(key1 = url) {
                    addImageViewModel.onEvent(LoadImage(url))
                }
            }
        }
        ImageFailDialog(url, appViewModel = appViewModel)
    }
}