package com.toolbox.admin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.admin.data.model.AdminUser
import com.toolbox.admin.ui.AdminViewModel
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
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AdminUser?>(null) }
    var usernameEdit by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.searchUsers("") }

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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users, key = { it.deviceId }) { user ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                user.username?.ifBlank { null } ?: user.deviceId.take(16),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${user.brand.orEmpty()} ${user.model.orEmpty()} · ${user.appVersion ?: "-"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "seen ${formatTs(user.lastSeen)} · ads ${user.adViews} · tools ${user.toolOpens}" +
                                    if (user.isBlocked == 1) " · BLOCKED" else "",
                                style = MaterialTheme.typography.bodySmall
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
