package com.example.gridtestapp.logic.states

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TopBarState (

    val showBack: Boolean,
    val showTopBar: Boolean,
    val title: String,

) : Parcelable