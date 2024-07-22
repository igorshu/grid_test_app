package com.example.gridtestapp.ui.navigation

import androidx.navigation.NavHostController
import com.example.gridtestapp.logic.events.AddImageScreenEvent
import com.example.gridtestapp.logic.events.ImageScreenEvent
import com.example.gridtestapp.logic.events.MainScreenEvent
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import org.koin.dsl.module
import java.net.URLEncoder

/**
 *
 *  Класс для навигации по приложению
 *
 */
class Routes() {

    private lateinit var navController: NavHostController

    fun navigate(route: String) {
        navController.navigate(route)
    }

    fun replaceToMain(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.MAIN) {
                inclusive = false
            }
        }
    }

    fun replaceToMain() {
        navController.navigate(Routes.MAIN) {
            popUpTo(Routes.MAIN) {
                inclusive = true
            }
        }
    }

    fun setController(navController: NavHostController) {
        this.navController = navController
    }

    fun goBack() {
        navController.navigateUp()
    }

    fun isImage() = IMAGE == navController.currentDestination?.route

    fun addListener(appViewModel: AppViewModel, navController: NavHostController):Boolean {
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when (destination.route) {
                MAIN -> {
                    appViewModel.setEvent(MainScreenEvent)
                }
                IMAGE -> {
                    arguments?.apply {
                        val url = getString("url")
                        val index = getString("index")?.toInt()
                        url?.let {
                            index?.let {
                                appViewModel.setEvent(ImageScreenEvent(url = url, index = index))
                            }
                        }
                    }
                }
                ADD_IMAGE -> {
                    val url = arguments?.getString("url")
                    url?.let {
                        appViewModel.setEvent(AddImageScreenEvent(url = url))
                    }
                }
            }
        }
        return true
    }

    companion object {

        const val MAIN = "main"
        const val IMAGE = "image/{index}/{url}"
        const val ADD_IMAGE = "add_image/{url}"

        fun imageRoute(url: String, index: Int): String {
            return IMAGE
                .replace("{url}", URLEncoder.encode(url, "UTF-8"))
                .replace("{index}", index.toString())
        }

        fun addImageRoute(url: String): String {
            return ADD_IMAGE
                .replace("{url}", URLEncoder.encode(url, "UTF-8"))
        }

        val module = module {
            single { Routes() }
        }
    }

}