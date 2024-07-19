package com.example.gridtestapp.ui.other

import androidx.compose.animation.core.*
import com.example.gridtestapp.logic.states.ImageState
import com.example.gridtestapp.logic.states.LoadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

const val animationDuration = 400
val easing = EaseInOutCubic

fun IntRange.size(): Int {
    return last - first
}

fun List<ImageState>.urls() = map { it.url }

fun List<ImageState>.index(url: String) = indexOfFirst { it.url == url }

fun List<ImageState>.notExists(url: String) : Boolean
    = firstOrNull { it.url == url && it.previewBitmap == null } != null

fun <T, R> StateFlow<T>.mapState(
    scope: CoroutineScope,
    transform: (value: T) -> R,
): StateFlow<R> =
    map(transform)
    .stateIn(
        scope,
        SharingStarted.Lazily,
        transform(value)
    )
