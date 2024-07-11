package com.example.gridtestapp.ui.other

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

@OptIn(ExperimentalSharedTransitionApi::class)
class Hero(val animatedScope: AnimatedVisibilityScope, val sharedTransitionScope: SharedTransitionScope) {
    infix fun <A, B> A.and(that: B): Pair<A, B> = Pair(this, that)
}