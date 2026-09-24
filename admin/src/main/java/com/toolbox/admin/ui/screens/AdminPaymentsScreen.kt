package com.toolbox.admin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.admin.data.model.AdminPayment
import com.toolbox.admin.ui.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminPaymentsScreen(
    vm: AdminViewModel,
    onBack: () -> Unit
) {
    val payments by vm.payments.collectAsState()
    var status by remember { mutableStateOf<String?>(null) }

    val filtered = if (status.isNullOrBlank()) payments else payments.filter { it.status == status }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payments", fontWeight = FontWeight.Bold) },
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, "created", "pending", "confirmed", "rejected").forEach { s ->
                    FilterChip(
                        selected = status == s,
                        onClick = { status = s },
                        label = { Text(s ?: "all") }
                    )
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { p ->
                    PaymentCard(p, onConfirm = { vm.confirmPayment(p.id) }, onReject = { vm.rejectPayment(p.id) })
                }
                if (filtered.isEmpty()) {
                    item { Text("No payments", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun PaymentCard(p: AdminPayment, onConfirm: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "${p.username?.ifBlank { null } ?: (p.deviceId?.take(12) ?: p.userId.take(12))} · ${p.method.uppercase()}",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${p.amount} · ${p.status} · ${p.plan}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!p.txHash.isNullOrBlank()) {
                Text("tx: ${p.txHash}", style = MaterialTheme.typography.bodySmall)
            }
            if (!p.note.isNullOrBlank()) {
                Text("note: ${p.note}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                formatTs(p.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (p.status == "created" || p.status == "pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onConfirm) { Text("Confirm") }
                    OutlinedButton(onClick = onReject) { Text("Reject") }
                }
            }
        }
    }
}

private fun formatTs(ts: Long): String {
    if (ts <= 0) return "-"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(ts))
}
