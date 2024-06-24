package com.example.gridtestapp.logic.events

/**
 *  Классы событий
 */

// События топ бара

sealed class TopBarEvent
data object ToggleTopBar : TopBarEvent()
data class MainTopBar(val url: String) : TopBarEvent()
data object ImageTopBar : TopBarEvent()

// События основного экрана

sealed class MainEvent
class ChangeVisibleIndexes(val indexesOnScreen: HashSet<Int>, val index: Int?) : MainEvent()
class UpdateImageWidthEvent(val width: Int) : MainEvent()

// События экрана с картинкой

sealed class ImageEvent
data object ToggleBars : ImageEvent()

