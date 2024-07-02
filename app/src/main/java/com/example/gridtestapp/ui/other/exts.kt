package com.example.gridtestapp.ui.other

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.gridtestapp.logic.events.OnMainEvent
import com.example.gridtestapp.logic.events.UpdateImageWidthEvent
import com.example.gridtestapp.logic.states.MainScreenState

fun Modifier.onWidthChanged(mainState: MainScreenState, onEvent: OnMainEvent): Modifier =
    this.onGloballyPositioned { coordinates ->
        if (!mainState.widthConsumed) {
            onEvent(UpdateImageWidthEvent(coordinates.size.width))
        }
    }