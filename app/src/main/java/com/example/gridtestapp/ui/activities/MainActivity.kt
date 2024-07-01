package com.example.gridtestapp.ui.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.os.Build
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
import com.example.gridtestapp.core.NotificationService
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.logic.viewmodels.ImageViewModel
import com.example.gridtestapp.logic.viewmodels.MainViewModel
import com.example.gridtestapp.ui.composables.TopBar
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.screens.ImageContent
import com.example.gridtestapp.ui.screens.MainContent
import com.example.gridtestapp.ui.theme.GridTestAppTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.get

class MainActivity : ComponentActivity() {

    val notificationService: NotificationService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        createNotificationChannel()

        setContent {
            GridTestAppTheme {
                val navController = rememberNavController()
                val routes = get<Routes>()
                val appViewModel = get<AppViewModel>()

                routes.setController(navController, appViewModel.onEvent)

                val appState = appViewModel.state.collectAsState()

                Scaffold(
                    topBar = { TopBar(appState.value, routes, appViewModel.onEvent) },
                ) { paddingValues ->
                    NavHost(
                        modifier = Modifier.padding(paddingValues),
                        navController = navController,
                        startDestination = Routes.MAIN,
                    ) {
                        composable(Routes.MAIN) {
                            val mainViewModel = get<MainViewModel>()
                            val mainState = mainViewModel.state.collectAsState()

                            MainContent(
                                mainState = mainState.value,
                                appState = appState.value,
                                onMainEvent = mainViewModel.onEvent,
                                onAppEvent = appViewModel.onEvent,
                                )
                        }
                        composable(Routes.IMAGE) { backStackEntry ->
                            val mainViewModel = get<MainViewModel>()

                            val url = backStackEntry.arguments?.getString("url")
                            val index = backStackEntry.arguments?.getString("index")!!.toInt()
                            val imageViewModel = remember {
                                val urls = appState.value.urls
                                ImageViewModel(urls, application, index to (url!!))
                            }

                            val imageState = imageViewModel.state.collectAsState()
                            val mainState = mainViewModel.state.collectAsState()

                            ImageContent(
                                imageState.value,
                                appState.value,
                                mainState.value,
                                imageViewModel.onEvent,
                                appViewModel.onEvent,
                                routes,
                                paddingValues)
                        }
                    }
                }
            }
        }
    }



    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NotificationService.NOTIFICATION_CHANNEL, "Application Notification", IMPORTANCE_HIGH)
            notificationChannel.description = "description"

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onStop() {
        super.onStop()

        notificationService.hideNotification(this)
    }
}
