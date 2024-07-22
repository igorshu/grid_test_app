package com.example.gridtestapp.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun ImageLoader(dpWidth: Dp) {
    Box(
        modifier = if (dpWidth == 0.dp) {
            Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        } else {
            Modifier
                .width(dpWidth)
                .height(dpWidth)
        }
            .zIndex(1f),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.fillMaxSize(0.25f))
    }
}
