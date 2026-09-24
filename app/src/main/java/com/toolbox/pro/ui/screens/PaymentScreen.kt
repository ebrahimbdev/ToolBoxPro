package com.toolbox.pro.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.toolbox.pro.core.api.ApiResult
import com.toolbox.pro.core.api.AppApi
import com.toolbox.pro.core.config.RemoteConfigRepository
import com.toolbox.pro.core.identity.IdentityEntryPoint
import com.toolbox.pro.core.localization.LocalStrings
import com.toolbox.pro.monetization.admob.BannerAd
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(navController: NavHostController? = null) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val entry = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                IdentityEntryPoint::class.java
            )
        }.getOrNull()
    }
    val remoteConfig = entry?.remoteConfigRepository()
    val config by if (remoteConfig != null) {
        remoteConfig.config.collectAsState()
    } else {
        remember { mutableStateOf(com.toolbox.pro.core.api.RemoteConfigDto()) }
    }

    var selectedMethod by remember { mutableStateOf<String?>(null) }
    var txHash by remember { mutableStateOf("") }
    var cardNote by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(remoteConfig) { remoteConfig?.refresh() }

    val api = remember { AppApi() }

    fun submitPayment(method: String) {
        val identityEntry = entry ?: return
        scope.launch {
            loading = true
            val identity = identityStoreSnapshot(identityEntry)
            val result = api.createPayment(
                identity = identity,
                method = method,
                amount = config.subscriptionPriceToman,
                plan = "monthly",
                txHash = txHash.trim(),
                note = cardNote.trim()
            )
            loading = false
            when (result) {
                is ApiResult.Success -> {
                    showSuccess = true
                    selectedMethod = null
                    txHash = ""
                    cardNote = ""
                    remoteConfig?.registerAndHeartbeat()
                }
                is ApiResult.Failure -> {
                    snackbar.showSnackbar(result.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.upgradeTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.freeTrialAds, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${config.subscriptionPriceToman} ${s.toman} · ${config.subscriptionDurationDays} ${s.days}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(s.removeAdsDesc, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }

            // Iranian Gateway - coming soon
            MethodCard(
                icon = Icons.Filled.AccountBalance,
                title = s.payGateway,
                subtitle = if (config.gatewayEnabled) s.payGatewayReady else s.comingSoon,
                enabled = false,
                onClick = { selectedMethod = "gateway" }
            )

            // Crypto
            MethodCard(
                icon = Icons.Filled.AccountBalanceWallet,
                title = s.payCrypto,
                subtitle = config.cryptoNetwork.ifBlank { "TRC20" },
                enabled = true,
                onClick = { selectedMethod = "crypto" }
            )

            // Card to card
            MethodCard(
                icon = Icons.Filled.CreditCard,
                title = s.payCard,
                subtitle = s.payCardManual,
                enabled = true,
                onClick = { selectedMethod = "card" }
            )

            BannerAd(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }

    // Crypto dialog
    if (selectedMethod == "crypto") {
        AlertDialog(
            onDismissRequest = { selectedMethod = null },
            title = { Text(s.payCrypto) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${s.cryptoNetwork}: ${config.cryptoNetwork}")
                    Text(s.sendToAddress, style = MaterialTheme.typography.bodyMedium)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                config.cryptoWallet.ifBlank { s.walletNotSet },
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            IconButton(onClick = {
                                copyToClipboard(context, config.cryptoWallet)
                            }) {
                                Icon(Icons.Filled.ContentCopy, null)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = txHash,
                        onValueChange = { txHash = it },
                        label = { Text(s.txHash) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        s.waitConfirm,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !loading && txHash.isNotBlank() && config.cryptoWallet.isNotBlank(),
                    onClick = { submitPayment("crypto") }
                ) {
                    if (loading) CircularProgressIndicator(Modifier.height(18.dp)) else Text(s.submit)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMethod = null }) { Text(s.cancel) }
            }
        )
    }

    // Card dialog
    if (selectedMethod == "card") {
        AlertDialog(
            onDismissRequest = { selectedMethod = null },
            title = { Text(s.payCard) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(s.cardInfo, style = MaterialTheme.typography.bodyMedium)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    config.cardNumber.ifBlank { s.cardNotSet },
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { copyToClipboard(context, config.cardNumber) }) {
                                    Icon(Icons.Filled.ContentCopy, null)
                                }
                            }
                            if (config.cardHolder.isNotBlank()) {
                                Text(config.cardHolder, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "${config.subscriptionPriceToman} ${s.toman}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    OutlinedTextField(
                        value = cardNote,
                        onValueChange = { cardNote = it },
                        label = { Text(s.payNote) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Text(s.waitConfirm, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !loading && config.cardNumber.isNotBlank(),
                    onClick = { submitPayment("card") }
                ) {
                    if (loading) CircularProgressIndicator(Modifier.height(18.dp)) else Text(s.submit)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMethod = null }) { Text(s.cancel) }
            }
        )
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = { showSuccess = false },
            title = { Text(s.paymentSubmitted) },
            text = { Text(s.waitConfirm) },
            confirmButton = {
                TextButton(onClick = { showSuccess = false }) { Text(s.done) }
            }
        )
    }
}

@Composable
private fun MethodCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!enabled) {
                Text(sComingSoon(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun sComingSoon(): String = LocalStrings.current.comingSoon

private fun copyToClipboard(context: Context, text: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("toolbox", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private suspend fun identityStoreSnapshot(
    entry: com.toolbox.pro.core.identity.IdentityEntryPoint
): com.toolbox.pro.core.identity.DeviceIdentity {
    return entry.identityStore().snapshot()
}
