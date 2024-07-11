package com.example.gridtestapp.logic.events

/**
 *  Классы событий
 */

// События приложения

sealed class AppEvent
data object MainScreenEvent : AppEvent()
data class ImageScreenEvent(val url: String, val index: Int) : AppEvent()
data class AddImageScreenEvent(val url: String) : AppEvent()
data object ToggleFullScreen : AppEvent()
data class SharePressed(val url: String) : AppEvent()
data class RemoveImage(val url: String, val index: Int) : AppEvent()
data class ShowImageFailDialog(val url: String) : AppEvent()
data object DismissImageFailDialog : AppEvent()
data class LoadImageAgain(val url: String) : AppEvent()
data class ChangeVisibleIndexes(val indexesOnScreen: HashSet<Int>, val index: Int?) : AppEvent()
data object AppResumed : AppEvent()
data object AppPaused : AppEvent()
data class ChangeTheme(val index: Int) : AppEvent()
data object Reload : AppEvent()
data class GotUrlIntent(val url: String) : AppEvent()
data class UpdateCurrentImage(val url: String, val index: Int) : AppEvent()
data class AddImage(val url: String) : AppEvent()
data class ImagePressed(val url: String, val index: Int) : AppEvent()


// События основного экрана

sealed class MainEvent
data class UpdateImageWidthEvent(val width: Int) : MainEvent()

// События экрана с картинкой

sealed class ImageEvent
data class LoadOriginalImageFromDisk(val url: String, val index: Int): ImageEvent()
data class ShowImageNotification(val url: String): ImageEvent()

// События экрана get shared url

sealed class AddImageEvent
data class LoadImage(val url: String): AddImageEvent()
data object CancelAdd: AddImageEvent()