package com.example.gridtestapp.ui.other

import androidx.compose.animation.core.*
import com.example.gridtestapp.logic.states.ImageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.random.Random

const val animationDuration = 400
val easing = EaseInOutCubic

fun IntRange.size(): Int {
    return last - first
}

fun List<MutableStateFlow<ImageState>>.urls() = map { it.value.url }

fun List<StateFlow<ImageState>>.index(url: String) = indexOfFirst { it.value.url == url }

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

fun Float.shake(r: Int = 5): Float = this + Random.nextInt(-r, r)

fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    val tmp = this[index1]
    this[index1] = this[index2]
    this[index2] = tmp
}

fun ImageState.id() = url