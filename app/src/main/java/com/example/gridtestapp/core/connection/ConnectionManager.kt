package com.example.gridtestapp.core.connection

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat.getSystemService
import com.example.gridtestapp.logic.viewmodels.AppViewModel.Companion.MAIN_URL
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.dsl.module
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class ConnectionManager(private val application: Application): KoinComponent {

    var online: Boolean = true

    private val _state: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val state: Flow<ConnectionState>
        get() = _state
            .onEach { online = it }
            .runningFold(initial = ConnectionState(true, true)) { a, b -> ConnectionState(a.current, b) }

    suspend fun init() {
        initConnectivity(application)
        startPinger()
    }

    private suspend fun startPinger(): Nothing {
        while (true) {
            delay(15_000) // 15 seconds
            checkSiteUrl()
        }
    }

    private fun checkSiteUrl() {
        try {
            val url = URL(MAIN_URL)
            val connection: HttpsURLConnection = url.openConnection() as HttpsURLConnection
            connection.connect()
            connection.disconnect()

            _state.update { true }
        } catch (e: Exception) {
            _state.update {  false }
        }
    }

    private fun initConnectivity(application: Application) {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                _state.update { true }
            }

            override fun onUnavailable() {
                super.onUnavailable()

                _state.update { false }
            }

            override fun onLost(network: Network) {
                super.onLost(network)

                _state.update { false }
            }
        }

        val connectivityManager = getSystemService(application, ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }

    suspend fun listenRestore(onRestore: () -> Unit) {
        return state
            .filter { connectionState ->
                !connectionState.previous && connectionState.current
            }.collect { onRestore() }
    }

    companion object {
        val module = module {
            single { ConnectionManager(get()) }
        }
    }

}