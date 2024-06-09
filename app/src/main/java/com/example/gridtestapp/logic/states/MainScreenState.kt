package com.example.gridtestapp.logic.states

import android.graphics.Bitmap

data class MainScreenState(
    var urls: List<String>,
    val loadedUrls: HashSet<String>,
)