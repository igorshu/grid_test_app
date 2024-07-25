package com.example.gridtestapp.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.gridtestapp.R
import com.example.gridtestapp.logic.events.ShowImageFailDialog
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import org.koin.androidx.compose.get

@Composable
fun FailBox(
    url: String,
    dpWidth: Dp,
) {
    val appViewModel: AppViewModel = get()

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
            .zIndex(1f)
            .clickable { appViewModel.setEvent(ShowImageFailDialog(url)) }
            .background(MaterialTheme.colorScheme.background)
        ,
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(id = R.string.error))
    }
}