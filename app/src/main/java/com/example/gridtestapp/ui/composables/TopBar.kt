package com.example.gridtestapp.ui.composables

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
import androidx.compose.ui.graphics.Color
import com.example.gridtestapp.logic.events.SharePressed
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.states.Screen
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    appState: State<AppState>,
    routes: Routes,
    appViewModel: AppViewModel
) {
    if (appState.value.showTopBar) {
        TopAppBar(
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
                    IconButton(onClick = { appViewModel.onEvent(SharePressed) }) {
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