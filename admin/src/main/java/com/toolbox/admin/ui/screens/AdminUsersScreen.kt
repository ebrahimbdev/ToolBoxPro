package com.toolbox.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.admin.data.model.AdminUser
import com.toolbox.admin.ui.AdminViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    vm: AdminViewModel,
    onBack: () -> Unit
) {
    val users by vm.users.collectAsState()
    val live by vm.live.collectAsState()
    val lastRefreshAt by vm.lastRefreshAt.collectAsState()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AdminUser?>(null) }
    var usernameEdit by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) { vm.startLive() }
    DisposableEffect(Unit) { onDispose { vm.stopLive() } }
    // Ticks once a second so "online / updated Xs ago" stay accurate.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }

    if (selected != null) {
        val user = selected!!
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(user.username?.ifBlank { null } ?: user.model ?: user.deviceId.take(12)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Device: ${user.deviceId}", style = MaterialTheme.typography.bodySmall)
                    Text("Model: ${user.brand.orEmpty()} ${user.model.orEmpty()}")
                    Text("Last seen: ${formatTs(user.lastSeen)}")
                    Text("Ad views: ${user.adViews} · Tools: ${user.toolOpens}")
                    Text("Blocked: ${user.isBlocked == 1}")
                    OutlinedTextField(
                        value = usernameEdit,
                        onValueChange = { usernameEdit = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateUsername(user.deviceId, usernameEdit)
                    selected = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        vm.toggleBlock(user)
                        selected = null
                    }) { Text(if (user.isBlocked == 1) "Unblock" else "Block") }
                    TextButton(onClick = {
                        vm.grant(user, 30)
                        selected = null
                    }) { Text("+30d") }
                    TextButton(onClick = { selected = null }) { Text("Close") }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Users", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    vm.searchUsers(it)
                },
                label = { Text("Search username / device / model") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.fillMaxWidth()
            )

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
                        text = if (live) "LIVE · ${ago(lastRefreshAt, now)}" else "paused",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (live) Color(0xFF2E7D35)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${users.count { it.lastSeen > 0 && now - it.lastSeen <= 120_000 }} online",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { vm.searchUsers(query) }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh now")
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users, key = { it.deviceId }) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    user.username?.ifBlank { null } ?: user.deviceId.take(16),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusBadge(user = user, now = now)
                            }
                            Text(
                                "${user.brand.orEmpty()} ${user.model.orEmpty()} · ${user.appVersion ?: "-"}" +
                                    if (user.osVersion.isNullOrBlank()) "" else " · Android ${user.osVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Last seen ${formatTs(user.lastSeen)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Ads watched: ${user.adViews} · Tasks done: ${user.toolOpens}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    selected = user
                                    usernameEdit = user.username.orEmpty()
                                }) { Text("Manage") }
                                OutlinedButton(onClick = { vm.grant(user, 30) }) { Text("+30d") }
                                OutlinedButton(onClick = { vm.toggleBlock(user) }) {
                                    Text(if (user.isBlocked == 1) "Unblock" else "Block")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTs(ts: Long): String {
    if (ts <= 0) return "-"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ts))
}

private fun ago(ts: Long, now: Long): String {
    if (ts <= 0L) return "waiting…"
    val sec = ((now - ts) / 1000).coerceAtLeast(0)
    return if (sec < 60) "updated ${sec}s ago" else "updated ${sec / 60}m ago"
}

/** Operating status: Blocked / Online (<=2 min) / Active 24h / Offline. */
@Composable
private fun StatusBadge(user: AdminUser, now: Long) {
    val age = if (user.lastSeen > 0) now - user.lastSeen else Long.MAX_VALUE
    val (label, color) = when {
        user.isBlocked == 1 -> "Blocked" to Color(0xFFC62828)
        age <= 120_000L -> "Online" to Color(0xFF2E7D32)
        age <= 24 * 60 * 60 * 1000L -> "Active 24h" to Color(0xFFF9A825)
        else -> "Offline" to Color(0xFF9E9E9E)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
