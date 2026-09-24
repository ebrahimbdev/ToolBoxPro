package com.toolbox.pro.ui.screens

import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.view.WindowManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.BatteryManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.pro.core.localization.LocalStrings
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class DeviceInfoItem(
    val icon: ImageVector,
    val label: String,
    val value: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen() {
    val s = LocalStrings.current
    val context = LocalContext.current

    val items = remember {
        getDeviceInfo(context, s)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.deviceInfo, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { item ->
                DeviceInfoRow(item)
            }
        }
    }
}

@Composable
private fun DeviceInfoRow(item: DeviceInfoItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getDeviceInfo(context: Context, s: com.toolbox.pro.core.localization.Strings): List<DeviceInfoItem> {
    val items = mutableListOf<DeviceInfoItem>()

    items.add(DeviceInfoItem(Icons.Filled.Palette, s.manufacturer, Build.MANUFACTURER))
    items.add(DeviceInfoItem(Icons.Filled.PhoneAndroid, s.brand, Build.BRAND))
    items.add(DeviceInfoItem(Icons.Filled.Android, s.model, Build.MODEL))
    items.add(DeviceInfoItem(Icons.Filled.Info, s.androidVersion, Build.VERSION.RELEASE + " (API ${Build.VERSION.SDK_INT})"))
    items.add(DeviceInfoItem(Icons.Filled.DeveloperBoard, s.sdk, Build.SUPPORTED_ABIS.joinToString(", ")))
    items.add(DeviceInfoItem(Icons.Filled.Memory, s.hardware, Build.HARDWARE ?: "Unknown"))

    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val memInfo = android.app.ActivityManager.MemoryInfo()
    am.getMemoryInfo(memInfo)
    val totalRamGb = memInfo.totalMem / (1024.0 * 1024 * 1024)
    val df = DecimalFormat("#.#", DecimalFormatSymbols(Locale.ROOT))
    items.add(DeviceInfoItem(Icons.Filled.Storage, s.ram, "${df.format(totalRamGb)} GB"))

    try {
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val metrics = android.util.DisplayMetrics()
        display.getMetrics(metrics)
        items.add(DeviceInfoItem(Icons.Filled.ScreenLockPortrait, s.screen, "${metrics.widthPixels} x ${metrics.heightPixels}"))
        items.add(DeviceInfoItem(Icons.Filled.ScreenLockPortrait, s.density, "${metrics.densityDpi} DPI"))
    } catch (_: Exception) {}

    try {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            else -> "Discharging"
        }
        items.add(DeviceInfoItem(Icons.Filled.BatteryFull, s.battery, "$level% ($statusText)"))
    } catch (_: Exception) {}

    return items
}
