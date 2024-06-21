package com.example.gridtestapp.logic.events

/**
 *  Классы событий
 */

// События основного экрана

sealed class MainEvent
class ChangeVisibleIndexes(val indexesOnScreen: HashSet<Int>, val index: Int?) : MainEvent()
class UpdateImageWidthEvent(val width: Int) : MainEvent()

// События экрана с картинкой

sealed class ImageEvent
data object ToggleBarsEvent : ImageEvent()

