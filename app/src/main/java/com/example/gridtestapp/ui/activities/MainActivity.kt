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

                val appState = appViewModel.state.collectAsState()

                routes.setController(navController, appViewModel::onEvent)

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

}
