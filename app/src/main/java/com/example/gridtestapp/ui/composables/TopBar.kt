package com.example.gridtestapp.ui.composables

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.gridtestapp.logic.events.Reload
import com.example.gridtestapp.logic.events.RemoveImage
import com.example.gridtestapp.logic.events.SharePressed
import com.example.gridtestapp.logic.states.Screen
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.ui.navigation.Routes
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    val appViewModel: AppViewModel = get()
    val appState by appViewModel.state.collectAsState()

    if (appState.showTopBar) {
        TopAppBar(
            modifier = Modifier,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                navigationIconContentColor = Color.White,
                titleContentColor = Color.White,
                containerColor = MaterialTheme.colorScheme.tertiary,
            ),
            title = { Text(appState.title, maxLines = 1) },
            navigationIcon = {
                if (appState.showBack) {
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
                when (appState.currentScreen) {
                    Screen.MAIN -> {
                        IconButton(onClick = { appViewModel.setEvent(Reload) }) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }

                    Screen.IMAGE -> {
                        IconButton(onClick = {
                            appState.currentImage?.let { imagePair ->
                                appViewModel.setEvent(RemoveImage(imagePair.url, imagePair.index))
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            appState.currentImage?.let { imagePair ->
                                appViewModel.setEvent(SharePressed(imagePair.url))
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                    }

                    Screen.ADD_IMAGE -> {}
                }
            }
        )
    }
}