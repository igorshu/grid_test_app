package com.example.gridtestapp.ui.navigation

import androidx.navigation.NavHostController
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

    fun setController(navController: NavHostController) {
        this.navController = navController
    }

    companion object {
        val module = module {
            single { Routes() }
        }
    }

}