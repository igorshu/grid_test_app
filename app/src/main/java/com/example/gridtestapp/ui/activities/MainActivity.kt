package com.example.gridtestapp.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gridtestapp.logic.viewmodels.ImageViewModel
import com.example.gridtestapp.logic.viewmodels.MainViewModel
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.ui.composables.TopBar
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.screens.ImageContent
import com.example.gridtestapp.ui.screens.MainContent
import com.example.gridtestapp.ui.theme.GridTestAppTheme
import org.koin.androidx.compose.get

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GridTestAppTheme {
                val navController = rememberNavController()
                val routes = get<Routes>()
                val appViewModel = get<AppViewModel>()

                routes.setController(navController, appViewModel::onEvent)

                val appState = appViewModel.state.collectAsState()
                Scaffold(
                    topBar = { TopBar(appState, routes, appViewModel::onEvent) },
                ) { paddingValues ->
                    NavHost(
                        modifier = Modifier.padding(paddingValues),
                        navController = navController,
                        startDestination = Routes.MAIN,
                    ) {
                        composable(Routes.MAIN) {
                            val mainViewModel = get<MainViewModel>()
                            MainContent(mainState = mainViewModel.state, onMainEvent = mainViewModel::onEvent)
                        }
                        composable(Routes.IMAGE) { backStackEntry ->
                            val mainViewModel = get<MainViewModel>()

                            val url = backStackEntry.arguments?.getString("url")
                            val index = backStackEntry.arguments?.getString("index")!!.toInt()
                            val imageViewModel = remember {
                                val urls = mainViewModel.state.value.urls
                                ImageViewModel(urls, application, index to (url!!))
                            }
                            ImageContent(
                                imageViewModel.state,
                                appViewModel.state,
                                mainViewModel.state,
                                imageViewModel.onEvent,
                                appViewModel::onEvent,
                                routes,
                                paddingValues)
                        }
                    }
                }
            }
        }
    }

}
