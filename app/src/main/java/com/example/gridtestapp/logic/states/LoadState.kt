package com.example.gridtestapp.logic.states

import androidx.compose.runtime.Immutable

@Immutable
enum class LoadState {
    IDLE, LOADED, LOADING, FAIL
}