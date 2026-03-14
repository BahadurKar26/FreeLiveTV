package com.rancon.freelivetv.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.net.ConnectivityManagerCompat

object NetworkUtils {

    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isMobileDataConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * Review 13: Detect if the network is metered (e.g. Mobile Hotspot).
     * Used to prevent excessive data drain during BDIX background scans.
     */
    fun isMetered(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return ConnectivityManagerCompat.isActiveNetworkMetered(connectivityManager)
    }

    fun getNetworkTypeName(context: Context): String {
        return when {
            isWifiConnected(context) -> if (isMetered(context)) "Metered Wi-Fi" else "Wi-Fi"
            isMobileDataConnected(context) -> "Mobile Data"
            else -> "Offline/Other"
        }
    }
}
