package com.example.gridtestapp.ui.other

import androidx.compose.animation.core.*
import com.example.gridtestapp.logic.states.Screen

const val animationDuration = 400

fun easing(screen: Screen): Easing {
    return when (screen) {
        Screen.MAIN -> {
            EaseInOutCubic
        }
        Screen.IMAGE -> {
            EaseInOutCubic
        }
        else -> {
            LinearEasing
        }
    }
}