package com.example.gridtestapp.ui.navigation

import androidx.navigation.NavHostController
import com.example.gridtestapp.logic.events.ImageScreenEvent
import com.example.gridtestapp.logic.events.MainScreenEvent
import com.example.gridtestapp.logic.events.OnTopBarEvent
import org.koin.dsl.module

/**
 *
 *  Класс для навигации по приложению
 *
 */
class Routes() {

    private lateinit var navController: NavHostController

    fun <T: Any> navigate(route: T) {
        navController.navigate(route)
    }

    fun setController(
        navController: NavHostController,
        onTopBarEvent: OnTopBarEvent
    ) {
        this.navController = navController

        navController.addOnDestinationChangedListener { _, destination, arguments ->
            if (destination.route?.contains("ImageScreen") == true) {
                onTopBarEvent(ImageScreenEvent(url = arguments!!.getString("url")!!))
            } else {
                onTopBarEvent(MainScreenEvent)
            }
        }
    }

    fun goBack() {
        navController.navigateUp()
    }

    companion object {
        val module = module {
            single { Routes() }
        }
    }

}