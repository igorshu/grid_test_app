package com.example.gridtestapp.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gridtestapp.logic.events.SharePressed
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.states.Screen
import com.example.gridtestapp.logic.viewmodels.ImageViewModel
import com.example.gridtestapp.logic.viewmodels.MainViewModel
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.screens.ImageContent
import com.example.gridtestapp.ui.screens.MainContent
import com.example.gridtestapp.ui.theme.GridTestAppTheme
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GridTestAppTheme {
                val navController = rememberNavController()
                val routes = get<Routes>()
                val appViewModel = get<AppViewModel>()

                val topBarState = appViewModel.state.collectAsState()

                routes.setController(navController, appViewModel::onEvent)

                Scaffold(
                    topBar = { TopBar(topBarState, routes, appViewModel) }
                ) { paddingValues ->
                    NavHost(
                        modifier = Modifier.padding(paddingValues),
                        navController = navController,
                        startDestination = Routes.MAIN,
                    ) {
                        composable(Routes.MAIN) {
                            val mainViewModel = get<MainViewModel>()
                            MainContent(mainState = mainViewModel.state, onEvent = mainViewModel::onEvent)
                        }
                        composable(Routes.IMAGE) { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url")
                            val imageViewModel = remember {
                                ImageViewModel(application, url!!)
                            }
                            ImageContent(
                                imageViewModel.state,
                                appViewModel.state,
                                imageViewModel::onEvent,
                                appViewModel::onEvent,
                                routes,
                                paddingValues)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TopBar(
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
}
