package com.example.gridtestapp.logic.states

import androidx.compose.ui.graphics.ImageBitmap

data class ImageScreenState(
    val showTopBar: Boolean,
    val imageLoaded: Boolean,
    val image: ImageBitmap?,
    )