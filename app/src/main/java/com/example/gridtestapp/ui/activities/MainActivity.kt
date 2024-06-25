package com.example.gridtestapp.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.gridtestapp.logic.viewmodels.ImageViewModel
import com.example.gridtestapp.logic.viewmodels.MainViewModel
import com.example.gridtestapp.logic.viewmodels.TopBarViewModel
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.screens.ImageContent
import com.example.gridtestapp.ui.screens.ImageScreen
import com.example.gridtestapp.ui.screens.MainContent
import com.example.gridtestapp.ui.screens.MainScreen
import com.example.gridtestapp.ui.theme.GridTestAppTheme
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GridTestAppTheme {
                val navController = rememberNavController()
                val routes = get<Routes>()
                val topBarViewModel = get<TopBarViewModel>()

                val topBarState = topBarViewModel.state.collectAsState()

                routes.setController(navController, topBarViewModel::onEvent)

                Scaffold(
                    topBar = {
                        if (topBarState.value.showTopBar) {
                            TopAppBar(
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    navigationIconContentColor = Color.White,
                                    titleContentColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                ),
                                title = { Text(topBarState.value.title, maxLines = 1) },
                                navigationIcon = {
                                    if (topBarState.value.showBack) {
                                        IconButton(onClick = {routes.goBack()}) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowBack,
                                                contentDescription = "Localized description"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { paddingValues ->
                    NavHost(
                        modifier = Modifier.padding(paddingValues),
                        navController = navController,
                        startDestination = MainScreen,
                    ) {
                        composable<MainScreen> {
                            val mainViewModel = get<MainViewModel>()
                            MainContent(mainState = mainViewModel.state, onEvent = mainViewModel::onEvent)
                        }
                        composable<ImageScreen> {
                            val (url) = remember {
                                it.toRoute<ImageScreen>()
                            }
                            val imageViewModel = remember {
                                ImageViewModel(application, url)
                            }
                            ImageContent(imageViewModel.state, imageViewModel::onEvent, topBarViewModel::onEvent, paddingValues)
                        }
                    }
                }
            }
        }
    }
}
