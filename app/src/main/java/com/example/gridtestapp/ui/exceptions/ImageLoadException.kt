package com.example.gridtestapp.ui.exceptions

/*
*
*   Исключение срабатывает в случае ошибки загрузки картинки
*
*/
class ImageLoadException (val url: String) : Exception()