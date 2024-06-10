package com.example.gridtestapp.logic.events

/**
 *  Классы событий
 */

// События основного экрана

sealed class MainEvent
class LoadImageEvent(val url: String) : MainEvent()
class ReloadImageEvent(val url: String) : MainEvent()
class UpdateImageWidthEvent(val width: Int) : MainEvent()

// События экрана с картинкой

sealed class ImageEvent
data object ToggleBarsEvent : ImageEvent()

