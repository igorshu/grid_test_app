package com.example.gridtestapp.ui.navigation

import androidx.navigation.NavHostController
import com.example.gridtestapp.logic.events.ImageScreenEvent
import com.example.gridtestapp.logic.events.MainScreenEvent
import com.example.gridtestapp.logic.events.OnAppBarEvent
import org.koin.dsl.module

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
        onAppBarEvent: OnAppBarEvent
    ) {
        this.navController = navController

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            if (destination.route == MAIN) {
                onAppBarEvent(MainScreenEvent)
            } else if (destination.route == IMAGE) {
                onAppBarEvent(ImageScreenEvent(url = arguments!!.getString("url")!!))
            }
        }
    }

    fun goBack() {
        navController.navigateUp()
    }

    companion object {
        const val MAIN = "main"
        const val IMAGE = "image/{url}"

        val module = module {
            single { Routes() }
        }
    }

}