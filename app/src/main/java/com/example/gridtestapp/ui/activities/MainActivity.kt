package com.example.gridtestapp.ui.activities

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gridtestapp.R
import com.example.gridtestapp.core.NotificationsManager
import com.example.gridtestapp.logic.events.AppPaused
import com.example.gridtestapp.logic.events.AppResumed
import com.example.gridtestapp.logic.events.ImageScreenEvent
import com.example.gridtestapp.logic.events.MainScreenEvent
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.ui.composables.TopBar
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.navigation.Routes.Companion.IMAGE
import com.example.gridtestapp.ui.navigation.Routes.Companion.MAIN
import com.example.gridtestapp.ui.screens.ImageContent
import com.example.gridtestapp.ui.screens.MainContent
import com.example.gridtestapp.ui.theme.GridTestAppTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.get
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val notificationsManager: NotificationsManager by inject()
    private val appViewModel: AppViewModel by viewModel()

    private fun addCallBackDispatcher() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                notificationsManager.cancelNotifications()
                finish()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        addCallBackDispatcher()

        notificationsManager.createNotificationChannel()

        setContent {
            val navController = rememberNavController()
            remember {
                val listener = OnDestinationChangedListener { controller, destination, arguments ->
                    if (destination.route == MAIN) {
                        appViewModel.onEvent(MainScreenEvent)
                    } else if (destination.route == IMAGE) {
                        appViewModel.onEvent(ImageScreenEvent(url = arguments?.getString("url") ?: ""))
                    }
                }
                navController.addOnDestinationChangedListener(listener)
                listener
            }
            val appState = appViewModel.state.collectAsState()

            val theme by remember {
                derivedStateOf {
                    appState.value.theme
                }
            }

            get<Routes>().setController(navController)

            GridTestAppTheme(
                darkTheme = theme.isDark(systemTheme = isSystemInDarkTheme())
            ) {
                Scaffold(
                    topBar = { TopBar(appViewModel = appViewModel) },
                ) { paddingValues ->

                    LifecycleResumeEffect(Unit) {
                        appViewModel.onEvent(AppResumed)
                        onPauseOrDispose {
                            appViewModel.onEvent(AppPaused)
                        }
                    }

                    NavHost(
                        modifier = Modifier,
                        navController = navController,
                        startDestination = Routes.MAIN,
                    ) {
                        composable(Routes.MAIN) {
                            MainContent(paddingValues, appViewModel = appViewModel)
                        }
                        composable(Routes.IMAGE) { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url")
                            val index = backStackEntry.arguments?.getString("index")?.toInt()

                            if (url != null && index != null) {
                                ImageContent(index, url, appState.value.urls, appViewModel = appViewModel)
                            } else {
                                val params = mutableListOf<String>()
                                    .apply {
                                        url ?: add("url")
                                        index ?: add("index")
                                    }
                                    .joinToString(", ")
                                val errorText = getString(R.string.missing_arguments_s, params)
                                Toast.makeText(applicationContext, errorText, Toast.LENGTH_LONG).show()
                            }

                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode) {
            NotificationsManager.PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    notificationsManager.showAppNotification()
                } else {
                    Toast.makeText(this, getString(R.string.please_notifications), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        notificationsManager.requestPermissions(this)
    }
}
