@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.gridtestapp

import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.gridtestapp.logic.viewmodels.ImageViewModel
import com.example.gridtestapp.logic.viewmodels.MainViewModel
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.screens.ImageContent
import com.example.gridtestapp.ui.screens.ImageScreen
import com.example.gridtestapp.ui.screens.MainContent
import com.example.gridtestapp.ui.screens.MainScreen
import com.example.gridtestapp.ui.theme.GridTestAppTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.compose.get

class MainActivity : ComponentActivity() {

    companion object {
        private val displayMetrics = Resources.getSystem().displayMetrics
        val minSide = minOf(displayMetrics.heightPixels, displayMetrics.widthPixels)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GridTestAppTheme {
                val navController = rememberNavController()
                get<Routes>().setController(navController)

                NavHost(
                    navController = navController,
                    startDestination = MainScreen,
                ) {
                    composable<MainScreen> {
                        val mainViewModel = get<MainViewModel>()
                        MainContent(mainState = mainViewModel.state, onEvent = mainViewModel::onEvent)
                    }
                    composable<ImageScreen> {
                        val (url) = it.toRoute<ImageScreen>()
                        val imageViewModel = get<ImageViewModel>()
                        ImageContent(url, imageViewModel.state, imageViewModel::onEvent)
                    }
                }
            }
        }
    }
}
