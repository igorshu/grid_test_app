package com.example.gridtestapp.ui.exceptions

/*
*
*   Исключение срабатывает в случае ошибки загрузки картинки
*
*/
class ImageLoadException internal constructor(
    val url: String,
    override val message: String?,
    val validUrl: Boolean,
    val innerException: Throwable? = null
) : Exception(message) {

    constructor(url: String, validUrl: Boolean, innerException: Throwable)
            : this(url, null, validUrl, innerException)

    constructor(url: String, message: String,  validUrl: Boolean)
            : this(url, message, validUrl, null)

}