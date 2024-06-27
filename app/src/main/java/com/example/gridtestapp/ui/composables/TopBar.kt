package com.example.gridtestapp.ui.composables

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import com.example.gridtestapp.logic.events.OnAppBarEvent
import com.example.gridtestapp.logic.events.SharePressed
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.states.Screen
import com.example.gridtestapp.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    appState: State<AppState>,
    routes: Routes,
    onEvent: OnAppBarEvent,
) {
    val initialTop = TopAppBarDefaults.windowInsets.getTop(LocalDensity.current)
    val top = remember { initialTop}

    if (appState.value.showTopBar) {

        val insets = WindowInsets(
            left = TopAppBarDefaults.windowInsets.getLeft(LocalDensity.current, LayoutDirection.Ltr),
            top = top,
            right = TopAppBarDefaults.windowInsets.getRight(LocalDensity.current, LayoutDirection.Ltr),
            bottom = TopAppBarDefaults.windowInsets.getBottom(LocalDensity.current),
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
                if (appState.value.showBack) {
                    IconButton(onClick = { routes.goBack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            },
            actions = {
                if (appState.value.currentScreen == Screen.IMAGE) {
                    IconButton(onClick = { onEvent(SharePressed) }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                }
            }
        )
    }
}