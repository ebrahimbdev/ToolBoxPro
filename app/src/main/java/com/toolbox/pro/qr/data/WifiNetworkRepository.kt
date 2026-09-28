package com.toolbox.pro.qr.data

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import com.toolbox.pro.fileshare.network.WifiUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class WifiNetwork(
    val ssid: String,
    val security: String,
    val isCurrent: Boolean,
    val isSaved: Boolean
)

@Singleton
class WifiNetworkRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Reads the connected network plus whatever saved networks Android still
     * exposes to a non-system app. Never requests a location permission: on
     * restricted builds the saved list simply comes back empty and the UI
     * falls back to manual entry.
     */
    fun listNetworks(): List<WifiNetwork> {
        val networks = LinkedHashMap<String, WifiNetwork>()

        val currentSsid = WifiUtils.getWifiSsid(context)
            ?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" && it != "<unknown>" }
        if (currentSsid != null) {
            networks[currentSsid] = WifiNetwork(
                ssid = currentSsid,
                security = "WPA",
                isCurrent = true,
                isSaved = true
            )
        }

        try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return networks.values.toList()

            @Suppress("DEPRECATION")
            val configured = wifiManager.configuredNetworks ?: emptyList()
            for (config in configured) {
                val ssid = config.SSID?.removeSurrounding("\"")?.takeIf { it.isNotBlank() } ?: continue
                if (networks.containsKey(ssid)) continue
                networks[ssid] = WifiNetwork(
                    ssid = ssid,
                    security = securityOf(config),
                    isCurrent = false,
                    isSaved = true
                )
            }
        } catch (_: SecurityException) {
            // Android 10+ hides saved networks from regular apps.
        } catch (_: Throwable) {
            // Older/odd OEM builds: manual entry stays available.
        }

        return networks.values.sortedWith(
            compareByDescending<WifiNetwork> { it.isCurrent }
                .thenBy { it.ssid.lowercase() }
        )
    }

    private fun securityOf(config: WifiConfiguration): String {
        val keyMgmt = config.allowedKeyManagement ?: return "WPA"
        return when {
            keyMgmt.get(WifiConfiguration.KeyMgmt.WPA_PSK) ||
                keyMgmt.get(WifiConfiguration.KeyMgmt.WPA2_PSK) ||
                keyMgmt.get(WifiConfiguration.KeyMgmt.SAE) -> "WPA"
            config.wepKeys?.any { it != null } == true -> "WEP"
            else -> "nopass"
        }
    }

    fun buildPayload(ssid: String, security: String, password: String): String {
        val escapedSsid = escape(ssid)
        return when (security) {
            "nopass" -> "WIFI:T:nopass;S:$escapedSsid;;"
            "WEP" -> "WIFI:T:WEP;S:$escapedSsid;P:${escape(password)};;"
            else -> "WIFI:T:WPA;S:$escapedSsid;P:${escape(password)};;"
        }
    }

    private fun escape(value: String): String = buildString(value.length + 4) {
        for (ch in value) {
            when (ch) {
                '\\', ';', ',', '"', ':' -> {
                    append('\\')
                    append(ch)
                }
                else -> append(ch)
            }
        }
    }
}
