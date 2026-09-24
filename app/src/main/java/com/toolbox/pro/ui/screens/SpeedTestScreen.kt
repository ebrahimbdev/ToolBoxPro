package com.toolbox.pro.ui.screens

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.pro.core.localization.LocalStrings
import com.toolbox.pro.fileshare.network.WifiUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject

data class SpeedTestUiState(
    val isTesting: Boolean = false,
    val progress: Float = 0f,
    val downloadSpeedMbps: Double? = null,
    val pingMs: Long? = null,
    val isWifiConnected: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SpeedTestViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SpeedTestUiState())
    val uiState: StateFlow<SpeedTestUiState> = _uiState.asStateFlow()

    fun checkWifi(context: Context) {
        _uiState.value = _uiState.value.copy(isWifiConnected = WifiUtils.isWifiConnected(context))
    }

    fun startTest(context: Context) {
        if (_uiState.value.isTesting) return
        viewModelScope.launch {
            _uiState.value = SpeedTestUiState(isTesting = true, isWifiConnected = WifiUtils.isWifiConnected(context))
            try {
                val ping = withContext(Dispatchers.IO) { measurePing() }
                _uiState.value = _uiState.value.copy(pingMs = ping)

                val speed = withContext(Dispatchers.IO) { measureDownloadSpeed { p ->
                    _uiState.value = _uiState.value.copy(progress = p)
                }}
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    downloadSpeedMbps = speed,
                    progress = 1f
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTesting = false,
                    error = e.message ?: "Error"
                )
            }
        }
    }

    private fun measurePing(): Long {
        val url = URL("https://www.gstatic.com/generate_204")
        val start = System.currentTimeMillis()
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "GET"
        conn.connect()
        conn.responseCode
        conn.disconnect()
        return System.currentTimeMillis() - start
    }

    private fun measureDownloadSpeed(onProgress: (Float) -> Unit): Double {
        val url = URL("https://speed.cloudflare.com/__down?bytes=5000000")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        conn.requestMethod = "GET"
        conn.connect()

        val totalSize = 5_000_000L
        var bytesRead = 0L
        val buffer = ByteArray(8192)
        val startTime = System.currentTimeMillis()

        conn.inputStream.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                bytesRead += read
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                if (elapsed > 0) {
                    onProgress((bytesRead.toFloat() / totalSize).coerceAtMost(1f))
                }
            }
        }
        conn.disconnect()

        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
        if (elapsed <= 0) return 0.0
        return (bytesRead * 8) / (elapsed * 1_000_000)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.checkWifi(context) }

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(500),
        label = "progress"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.speedTest, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isWifiConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Wifi, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(s.connectionRequired, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // Speed gauge
            Card(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    val sweepAngle = animatedProgress * 360f
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val strokeWidth = 16.dp.toPx()
                        val inset = strokeWidth / 2
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        // Background ring
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Progress ring
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(Color(0xFF4ECDC4), Color(0xFF44A08D), Color(0xFF4ECDC4))
                            ),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (uiState.isTesting) {
                            Text(
                                text = s.testing,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (uiState.downloadSpeedMbps != null) {
                            Text(
                                text = String.format(Locale.US, "%.1f", uiState.downloadSpeedMbps!!),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Mbps",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(s.downloadSpeed, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (uiState.isTesting) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
            }

            // Stats row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(label = s.ping, value = uiState.pingMs?.let { "$it ms" } ?: "--", modifier = Modifier.weight(1f))
                StatCard(label = s.downloadSpeed, value = uiState.downloadSpeedMbps?.let { String.format(Locale.US, "%.1f Mbps", it) } ?: "--", modifier = Modifier.weight(1f))
            }

            if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.startTest(context) },
                enabled = !uiState.isTesting && uiState.isWifiConnected,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (uiState.downloadSpeedMbps != null) s.restart else s.startTest, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
