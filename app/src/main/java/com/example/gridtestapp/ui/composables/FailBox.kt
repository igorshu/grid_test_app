package com.example.gridtestapp.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.gridtestapp.R
import com.example.gridtestapp.logic.events.ShowImageFailDialog
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import org.koin.androidx.compose.get

@Composable
fun FailBox(
    url: String,
    appViewModel: AppViewModel = get()
) {
    Box(
        modifier = Modifier
            .aspectRatio(1.0f)
            .clickable { appViewModel.onEvent(ShowImageFailDialog(url)) },
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(id = R.string.error))
    }
}