package com.example.gridtestapp.ui.exceptions

/*
*
*   Исключение срабатывает в случае ошибки загрузки картинки
*
*/
class ImageLoadException(val url: String, override val message: String?) : Exception(message)