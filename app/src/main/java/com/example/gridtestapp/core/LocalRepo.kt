package com.example.gridtestapp.core

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module

class LocalRepo(application: Application): KoinComponent {

    private val appName = application.getString(application.applicationInfo.labelRes)
    private val preferences =  application.getSharedPreferences(appName, MODE_PRIVATE)

    private val gson: Gson by inject()

    var theme: Int
        get() = preferences.getInt(THEME, 0)
        set(index) = preferences.edit { putInt(THEME, index) }

    var urls: List<String>?
        get() = preferences.readList(URLS)
        set(urls) = preferences.writeList(URLS, urls)

    private fun <T> SharedPreferences.writeList(key: String, data: List<T>?) {
        val json = gson.toJson(data)
        edit { putString(key, json) }
    }

    private inline fun <reified T> SharedPreferences.readList(key: String): List<T>? {
        val json = getString(key, null)
        val type = object : TypeToken<List<T>>() {}.type

        return try {
            gson.fromJson(json, type)
        } catch(e: JsonSyntaxException) {
            emptyList()
        }
    }

    private fun SharedPreferences.clear(key: String) {
        edit { remove(key) }
    }

    fun clearUrls() {
        preferences.clear(URLS)
    }


    companion object {

        const val URLS = "urls"
        const val THEME = "theme"

        val module = module {
            single { LocalRepo(get()) }
            single { Gson() }
        }
    }
}