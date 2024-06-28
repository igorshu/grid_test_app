package com.example.gridtestapp.logic.events

/**
 *  Классы событий
 */

// События топ бара

sealed class AppEvent
data object ToggleFullScreen : AppEvent()
data object SharePressed : AppEvent()
data object MainScreenEvent : AppEvent()
data class ImageScreenEvent(val url: String) : AppEvent()

// События основного экрана

sealed class MainEvent
data class ChangeVisibleIndexes(val indexesOnScreen: HashSet<Int>, val index: Int?) : MainEvent()
data class UpdateImageWidthEvent(val width: Int) : MainEvent()
data class ShowImageFailDialog(val url: String) : MainEvent()
data object DismissImageFailDialog : MainEvent()
data class LoadImageAgain(val url: String) : MainEvent()

// События экрана с картинкой

sealed class ImageEvent
data class LoadOriginalImageFromDisk(val url: String, val index: Int): ImageEvent()
