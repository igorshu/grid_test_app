package com.example.gridtestapp.ui.composables

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import com.example.gridtestapp.logic.events.Reload
import com.example.gridtestapp.logic.events.RemoveImage
import com.example.gridtestapp.logic.events.SharePressed
import com.example.gridtestapp.logic.states.Screen
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.ui.navigation.Routes
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    appViewModel: AppViewModel = get()
) {
    val appState = appViewModel.state.collectAsState()

    val windowInsets = TopAppBarDefaults.windowInsets
    val density = LocalDensity.current

    val initialTop = windowInsets.getTop(density)
    val top = remember { initialTop}

    val showTopBar by remember {
        derivedStateOf {
            appState.value.showTopBar
        }
    }

    if (showTopBar) {
        val layoutDirection = LayoutDirection.Ltr
        val insets = WindowInsets(
            left = windowInsets.getLeft(density, layoutDirection),
            top = top,
            right = windowInsets.getRight(density, layoutDirection),
            bottom = windowInsets.getBottom(density),
            )
        TopAppBar(
            modifier = Modifier,
            windowInsets = insets,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                navigationIconContentColor = Color.White,
                titleContentColor = Color.White,
                containerColor = MaterialTheme.colorScheme.tertiary,
            ),
            title = { Text(appState.value.title, maxLines = 1) },
            navigationIcon = {
                val showBack by remember {
                    derivedStateOf {
                        appState.value.showBack
                    }
                }
                if (showBack) {
                    val routes = get<Routes>()
                    IconButton(onClick = { routes.goBack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            },
            actions = {
                val currentScreen by remember {
                    derivedStateOf {
                        appState.value.currentScreen
                    }
                }
                when (currentScreen) {
                    Screen.MAIN -> {
                        IconButton(onClick = { appViewModel.onEvent(Reload) }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                    }
                    Screen.IMAGE -> {
                        IconButton(onClick = {
                            appState.value.currentImage?.let { imagePair ->
                                appViewModel.onEvent(RemoveImage(imagePair.url, imagePair.index))
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            appState.value.currentImage?.let { imagePair ->
                                appViewModel.onEvent(SharePressed(imagePair.url))
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        )
    }
}