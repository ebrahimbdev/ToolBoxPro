package com.toolbox.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.admin.ui.AdminViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    vm: AdminViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val stats by vm.stats.collectAsState()
    val ui by vm.ui.collectAsState()
    val live by vm.live.collectAsState()
    val lastRefreshAt by vm.lastRefreshAt.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) { vm.startLive() }
    DisposableEffect(Unit) { onDispose { vm.stopLive() } }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(ui.message, ui.error) {
        val msg = ui.message ?: ui.error
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { vm.refreshAll() }) { Icon(Icons.Filled.Refresh, "Refresh") }
                    IconButton(onClick = onLogout) { Icon(Icons.Filled.Logout, "Logout") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    if (live) Color(0xFF2E7D35) else Color(0xFF9E9E9E),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (live) "LIVE · updated" else "paused",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (live) Color(0xFF2E7D35)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = agoText(lastRefreshAt, now),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Users", "${stats?.totalUsers ?: 0}", Modifier.weight(1f))
                    StatCard(
                        "Online now",
                        "${stats?.onlineUsers ?: 0}",
                        Modifier.weight(1f),
                        highlight = Color(0xFF2E7D32)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Active 7d", "${stats?.activeUsers7d ?: 0}", Modifier.weight(1f))
                    StatCard("Premium", "${stats?.premiumUsers ?: 0}", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Ad views", "${stats?.totalAdViews ?: 0}", Modifier.weight(1f))
                    StatCard("Tool opens", "${stats?.totalToolOpens ?: 0}", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Pending", "${stats?.pendingPayments ?: 0}", Modifier.weight(1f))
                    StatCard("Revenue", "${stats?.confirmedRevenue ?: 0}", Modifier.weight(1f))
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text("Management", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            item {
                NavCard("Users", "View, search, grant subscription", Icons.Filled.Group) {
                    onNavigate("users")
                }
            }
            item {
                NavCard("Payments", "Confirm card / crypto payments", Icons.Filled.Payments) {
                    onNavigate("payments")
                }
            }
            item {
                NavCard("Remote Config", "Ads, prices, wallet, gateway", Icons.Filled.Settings) {
                    onNavigate("config")
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Color? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = highlight?.copy(alpha = 0.14f)
                ?: MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = highlight ?: MaterialTheme.colorScheme.onSurface
            )
            Text(title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun agoText(ts: Long, now: Long): String {
    if (ts <= 0L) return "waiting…"
    val sec = ((now - ts) / 1000).coerceAtLeast(0)
    return if (sec < 60) "updated ${sec}s ago" else "updated ${sec / 60}m ago"
}

@Composable
private fun NavCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.padding(start = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
