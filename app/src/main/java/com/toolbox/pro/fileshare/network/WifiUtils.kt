package com.toolbox.pro.fileshare.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface

object WifiUtils {

    fun getDeviceIpAddress(context: Context): String? {
        val wifiIp = getWifiIpAddress(context)
        if (wifiIp != null) return wifiIp
        return getNetworkInterfaceIp()
    }

    private fun getWifiIpAddress(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val ip = wifiManager.connectionInfo.ipAddress
            String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getNetworkInterfaceIp(): String? {
        return try {
            val enumeration = NetworkInterface.getNetworkInterfaces() ?: return null
            val interfaces = java.util.Collections.list(enumeration)
            for (networkInterface in interfaces) {
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addrs = java.util.Collections.list(networkInterface.inetAddresses)
                    for (addr in addrs) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            return addr.hostAddress
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun getWifiSsid(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo
            @Suppress("DEPRECATION")
            wifiInfo.ssid?.removeSurrounding("\"")
        } catch (e: Exception) {
            null
        }
    }

    fun isWifiConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (e: Exception) {
            false
        }
    }

    fun getNetworkSpeed(context: Context): String {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val linkSpeed = wifiManager.connectionInfo.linkSpeed
            "$linkSpeed Mbps"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
