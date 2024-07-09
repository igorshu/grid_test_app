package com.example.gridtestapp.logic.states

data class AddImageState internal constructor(
    val loadState: LoadState,
    val imageError: ImageError?,
)
{

    companion object {
        fun init(): AddImageState = AddImageState(
            LoadState.IDLE,
            imageError = null,
        )
    }
}