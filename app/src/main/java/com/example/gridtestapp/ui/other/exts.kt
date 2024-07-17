package com.example.gridtestapp.ui.other

import androidx.compose.animation.core.*

const val animationDuration = 400
val easing = EaseInOutCubic

fun IntRange.size(): Int {
    return last - first
}