package com.example.gridtestapp.ui.activities

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gridtestapp.R
import com.example.gridtestapp.core.NotificationsManager
import com.example.gridtestapp.logic.events.AppPaused
import com.example.gridtestapp.logic.events.AppResumed
import com.example.gridtestapp.logic.events.GotUrlIntent
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.ui.activities.SplashActivity.Companion.ADD_URL
import com.example.gridtestapp.ui.composables.TopBar
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.screens.AddImageContent
import com.example.gridtestapp.ui.screens.ImageContent
import com.example.gridtestapp.ui.screens.MainContent
import com.example.gridtestapp.ui.theme.GridTestAppTheme
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.get
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val notificationsManager: NotificationsManager by inject()
    private val appViewModel: AppViewModel by viewModel()
    private val routes: Routes by inject()

    private fun addCallBackDispatcher() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                notificationsManager.cancelNotifications()
                finish()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        parseIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        addCallBackDispatcher()

        notificationsManager.createNotificationChannel()

        setContent {
            val navController = rememberNavController()
            LaunchedEffect(key1 = navController) {
                routes.addListener(appViewModel, navController)
                parseIntent(intent)
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

                    val systemUiController: SystemUiController = rememberSystemUiController()
                    LaunchedEffect(key1 = appState.value.showSystemBars) {
                        setSystemBars(appState.value.showSystemBars, systemUiController)
                    }

                    NavHost(
                        modifier = Modifier,
                        navController = navController,
                        startDestination = Routes.MAIN,
                        enterTransition = { fadeIn(animationSpec = tween(400)) },
                        exitTransition = { fadeOut(animationSpec = tween(400)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                        popExitTransition = { fadeOut(animationSpec = tween(400)) },
                    ) {

                        composable(Routes.MAIN) {
                            MainContent(paddingValues, appViewModel = appViewModel)
                        }
                        composable(Routes.IMAGE) { backStackEntry ->
                            backStackEntry.arguments?.let { arguments ->
                                val url = arguments.getString("url")
                                val index = arguments.getString("index")?.toInt()

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
                            } ?: run {
                                val errorText = getString(R.string.missing_arguments_s, "url and index")
                                Toast.makeText(applicationContext, errorText, Toast.LENGTH_LONG).show()
                            }
                        }
                        composable(Routes.ADD_IMAGE) { backStackEntry ->
                            backStackEntry.arguments?.let { arguments ->
                                arguments.getString("url")?.let { url ->
                                    AddImageContent(paddingValues, url)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseIntent(intent: Intent) {
        intent.getStringExtra(ADD_URL)?.let { url ->
            appViewModel.onEvent(GotUrlIntent(url))
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

fun setSystemBars(show: Boolean, systemUiController: SystemUiController) {
    systemUiController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    systemUiController.isSystemBarsVisible = show
}