package com.example.gridtestapp.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridtestapp.R
import com.example.gridtestapp.logic.events.DismissImageFailDialog
import com.example.gridtestapp.logic.states.AppState
import com.example.gridtestapp.logic.viewmodels.AppViewModel
import org.koin.androidx.compose.get

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ImageFailDialog(
    url: String,
    onLoadAgain: () -> Unit,
) {
    val appViewModel: AppViewModel = get()
    val appState by appViewModel.state.collectAsState()

    val show = appState.showImageFailDialog.isSome { it == url }

    if (show) {
        val imageError = appState.imageStates[url]?.imageError
        imageError?.let {
            AlertDialog(onDismissRequest = { appViewModel.onEvent(DismissImageFailDialog) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 15.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(id = R.string.loading_error),
                            style = TextStyle(fontSize = 24.sp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(imageError.errorMessage)
                        Spacer(modifier = Modifier.height(5.dp))
                        if (imageError.canBeLoad) {
                            Button(
                                modifier = Modifier.padding(top = 15.dp),
                                onClick = {
                                    appViewModel.onEvent(DismissImageFailDialog)
                                    onLoadAgain.invoke()
                                },
                            ) {
                                Text(stringResource(id = R.string.load_again))
                            }
                        }
                        Button(
                            modifier = Modifier.padding(top = 15.dp),
                            onClick = { appViewModel.onEvent(DismissImageFailDialog) }
                        ) {
                            Text(stringResource(id = R.string.ok))
                        }
                    }
                }
            }
        }
    }
}