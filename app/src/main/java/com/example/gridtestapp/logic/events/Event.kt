package com.example.gridtestapp.logic.events

import androidx.compose.ui.unit.Dp

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
data class LoadImageAgain(val url: String, val index: Int) : AppEvent()
data class ChangeVisibleRange(val visibleRange: IntRange) : AppEvent()
data object AppResumed : AppEvent()
data object AppPaused : AppEvent()
data class ChangeTheme(val index: Int) : AppEvent()
data object Reload : AppEvent()
data class GotUrlIntent(val url: String) : AppEvent()
data class UpdateCurrentImage(val url: String, val index: Int) : AppEvent()
data class AddImage(val url: String) : AppEvent()
data class ImagePressed(val url: String, val index: Int) : AppEvent()
data class ImagePressedNavigate(val url: String, val index: Int) : AppEvent()
data object GoBackFromImage : AppEvent()
data object DisableSharedAnimation : AppEvent()

// События основного экрана

sealed class MainEvent
data class UpdateImageWidthEvent(val width: Int, val dpWidth: Dp) : MainEvent()

// События экрана с картинкой

sealed class ImageEvent
data class LoadOriginalImageFromDisk(val url: String, val index: Int): ImageEvent()
data class ShowImageNotification(val url: String): ImageEvent()

// События экрана get shared url

sealed class AddImageEvent
data class LoadImage(val url: String): AddImageEvent()
data object CancelAdd: AddImageEvent()