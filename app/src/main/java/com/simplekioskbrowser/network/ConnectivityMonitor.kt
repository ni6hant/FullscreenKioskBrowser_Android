package com.simplekioskbrowser.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest

/**
 * Monitors network connectivity using ConnectivityManager.NetworkCallback (lifecycle-managed by
 * the activity). Reports available/lost transitions; does not poll.
 */
class ConnectivityMonitor(
    private val context: Context,
    private val onAvailable: () -> Unit,
    private val onLost: () -> Unit
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun start() {
        stop()
        val request = NetworkRequest.Builder().build()
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                onAvailable()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                onLost()
            }
        }
        callback = cb
        connectivityManager.registerNetworkCallback(request, cb)
    }

    fun stop() {
        val cb = callback
        callback = null
        cb?.let { connectivityManager.unregisterNetworkCallback(it) }
    }
}
