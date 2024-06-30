package com.example.gridtestapp.ui.navigation

import androidx.navigation.NavHostController
import com.example.gridtestapp.logic.events.ImageScreenEvent
import com.example.gridtestapp.logic.events.MainScreenEvent
import com.example.gridtestapp.logic.events.OnAppEvent
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

    fun setController(
        navController: NavHostController,
        onAppEvent: OnAppEvent
    ) {
        this.navController = navController

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            if (destination.route == MAIN) {
                onAppEvent(MainScreenEvent)
            } else if (destination.route == IMAGE) {
                onAppEvent(ImageScreenEvent(url = arguments!!.getString("url")!!))
            }
        }
    }

    fun goBack() {
        navController.navigateUp()
    }

    fun isImage() = IMAGE == navController.currentDestination?.route

    companion object {

        const val MAIN = "main"
        const val IMAGE = "image/{index}/{url}"

        fun imageRoute(url: String, index: Int): String {
            return IMAGE
                .replace("{url}", URLEncoder.encode(url, "UTF-8"))
                .replace("{index}", index.toString())
        }

        val module = module {
            single { Routes() }
        }
    }

}