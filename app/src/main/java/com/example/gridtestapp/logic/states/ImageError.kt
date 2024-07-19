package com.example.gridtestapp.logic.states

import androidx.compose.runtime.Immutable

@Immutable
data class ImageError(
    val errorMessage: String,
    val canBeLoad: Boolean,
) {
    override fun toString(): String {
        return "ImageError(errorMessage='$errorMessage', canBeLoad=$canBeLoad)"
    }
}