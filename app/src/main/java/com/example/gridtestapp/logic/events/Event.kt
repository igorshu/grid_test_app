package com.example.gridtestapp.logic.events

/**
 *  Классы событий
 */

// События основного экрана

sealed class MainEvent
class LoadImageEvent(val url: String) : MainEvent()
class UpdateImageWidthEvent(val width: Int) : MainEvent()
class DisposeImageEvent(val url: String) : MainEvent()

// События экрана с картинкой

sealed class ImageEvent
data object ToggleBarsEvent : ImageEvent()

