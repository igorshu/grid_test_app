package com.example.gridtestapp.logic.states

sealed class LoadState
data object Loaded: LoadState()
data object Loading: LoadState()
data object Fail: LoadState()