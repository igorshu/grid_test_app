@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalSharedTransitionApi::class)

package com.example.gridtestapp.ui.activities

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.gridtestapp.ui.composables.TopBar
import com.example.gridtestapp.ui.navigation.Routes
import com.example.gridtestapp.ui.other.urls
import com.example.gridtestapp.ui.screens.AddImageContent
import com.example.gridtestapp.ui.screens.ImageContent
import com.example.gridtestapp.ui.screens.MainContent
import com.example.gridtestapp.ui.theme.GridTestAppTheme
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MainActivity : ComponentActivity(), KoinComponent {

    private val notificationsManager: NotificationsManager by inject()
    private val routes: Routes by inject()

    companion object {
        const val ADD_URL = "Add.Url"
    }

    private fun addCallBackDispatcher() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                notificationsManager.cancelNotifications()
                finish()
            }
        })
    }

    private fun parseIntent(intent: Intent) {
        intent.getStringExtra(ADD_URL)?.let { url ->
            get<AppViewModel>().setEvent(GotUrlIntent(url))
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        parseIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        addCallBackDispatcher()

        notificationsManager.createNotificationChannel()

        setContent {
            val appViewModel = get<AppViewModel>()

            SharedTransitionLayout() {
                val navController = rememberNavController()

                LaunchedEffect(key1 = navController) {
                    routes.addListener(appViewModel, navController)
                    parseIntent(intent)
                }

                val appState by appViewModel.state.collectAsState()
                val theme = appState.theme

                get<Routes>().setController(navController)

                GridTestAppTheme(
                    darkTheme = theme.isDark(systemTheme = isSystemInDarkTheme())
                ) {
                    Scaffold(
                        topBar = { TopBar() },
                    ) { paddingValues ->

                        LifecycleResumeEffect(Unit) {
                            appViewModel.setEvent(AppResumed)
                            onPauseOrDispose {
                                appViewModel.setEvent(AppPaused)
                            }
                        }

                        val showSystemBars = appState.showSystemBars

                        val systemUiController: SystemUiController = rememberSystemUiController()
                        LaunchedEffect(key1 = showSystemBars) {
                            setSystemBars(showSystemBars, systemUiController)
                        }

                        NavHost(
                            modifier = Modifier,
                            navController = navController,
                            startDestination = Routes.MAIN,

                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None },
                        ) {

                            composable(Routes.MAIN) {
                                MainContent(
                                    paddingValues,
                                    this@composable,
                                    this@SharedTransitionLayout,
                                )
                            }
                            composable(
                                Routes.IMAGE,
                                exitTransition = {
                                    if (!appViewModel.state.value.sharedAnimation) {
                                        fadeOut(tween(700))
                                    } else {
                                        ExitTransition.None
                                    }
                                },
                            ) { backStackEntry ->
                                backStackEntry.arguments?.let { arguments ->
                                    val url = arguments.getString("url")
                                    val index = arguments.getString("index")?.toInt()

                                    if (url != null && index != null) {
                                        ImageContent(
                                            index,
                                            url,
                                            appViewModel.imageStates.urls(),
                                            this@composable,
                                            this@SharedTransitionLayout,
                                            paddingValues = paddingValues,
                                        )
                                    } else {
                                        val params = mutableListOf<String>()
                                            .apply {
                                                url ?: add("url")
                                                index ?: add("index")
                                            }
                                            .joinToString(", ")
                                        val errorText =getString(R.string.missing_arguments_s, params)
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
                                        AddImageContent(
                                            paddingValues,
                                            url,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun setSystemBars(show: Boolean, systemUiController: SystemUiController) {
    systemUiController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    systemUiController.isSystemBarsVisible = show
}