package com.example.gridtestapp.ui.screens

import kotlinx.serialization.Serializable


/**
 *
 * Идентификаторы экранов для навигации
 *
 */

@Serializable
object MainScreen

@Serializable
data class ImageScreen(
    val url: String,
)