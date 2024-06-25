package com.example.gridtestapp.logic.events

/**
 *  Классы событий
 */

// События топ бара

sealed class AppEvent
data object ToggleTopBar : AppEvent()
data object ToggleSystemBars : AppEvent()
data object MainScreenEvent : AppEvent()
data class ImageScreenEvent(val url: String) : AppEvent()

// События основного экрана

sealed class MainEvent
class ChangeVisibleIndexes(val indexesOnScreen: HashSet<Int>, val index: Int?) : MainEvent()
class UpdateImageWidthEvent(val width: Int) : MainEvent()

// События экрана с картинкой

sealed class ImageEvent
