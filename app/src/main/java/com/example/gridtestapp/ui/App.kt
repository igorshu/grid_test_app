package com.example.gridtestapp.ui

import android.app.Application
import com.example.gridtestapp.core.NotificationsManager
import com.example.gridtestapp.core.cache.ImageLoader
import com.example.gridtestapp.logic.viewmodels.AddImageViewModel
import com.example.gridtestapp.logic.viewmodels.MainViewModel
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import com.example.gridtestapp.logic.viewmodels.ImageViewModel
import com.example.gridtestapp.ui.navigation.Routes
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin

/**
 *  Класс приложения
 */
class App: Application(), KoinComponent {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(
                Routes.module,
                MainViewModel.module,
                ImageViewModel.module,
                AppViewModel.module,
                NotificationsManager.module,
                ImageLoader.module,
                AddImageViewModel.module,
            )
        }
    }
}