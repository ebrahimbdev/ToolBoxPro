package com.toolbox.admin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.toolbox.admin.data.model.ConfigPatch
import com.toolbox.admin.ui.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConfigScreen(
    vm: AdminViewModel,
    onBack: () -> Unit
) {
    val config by vm.config.collectAsState()

    var adInterval by remember { mutableStateOf("") }
    var banner by remember { mutableStateOf(true) }
    var interstitial by remember { mutableStateOf(true) }
    var multiplier by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var cryptoWallet by remember { mutableStateOf("") }
    var cryptoNetwork by remember { mutableStateOf("") }
    var gatewayNote by remember { mutableStateOf("") }

    LaunchedEffect(config) {
        config?.let { c ->
            adInterval = c.adIntervalSeconds.toString()
            banner = c.bannerEnabled == 1
            interstitial = c.interstitialEnabled == 1
            multiplier = c.freeAdMultiplier.toString()
            price = c.subscriptionPriceToman.toString()
            duration = c.subscriptionDurationDays.toString()
            cardNumber = c.cardNumber
            cardHolder = c.cardHolder
            cryptoWallet = c.cryptoWallet
            cryptoNetwork = c.cryptoNetwork
            gatewayNote = c.gatewayStatusNote
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remote Config", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Ads", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = adInterval,
                onValueChange = { adInterval = it },
                label = { Text("Ad interval (seconds)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Banner enabled", Modifier.weight(1f))
                Switch(checked = banner, onCheckedChange = { banner = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Interstitial enabled", Modifier.weight(1f))
                Switch(checked = interstitial, onCheckedChange = { interstitial = it })
            }
            OutlinedTextField(
                value = multiplier,
                onValueChange = { multiplier = it },
                label = { Text("Free ad multiplier") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Subscription", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price (Toman)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Duration (days)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Card-to-card", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Card number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cardHolder,
                onValueChange = { cardHolder = it },
                label = { Text("Card holder") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Crypto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = cryptoWallet,
                onValueChange = { cryptoWallet = it },
                label = { Text("Wallet address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = cryptoNetwork,
                onValueChange = { cryptoNetwork = it },
                label = { Text("Network (TRC20, BEP20, ...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Iranian gateway", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Status note (shown as «به‌زودی» until merchant ready)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = gatewayNote,
                onValueChange = { gatewayNote = it },
                label = { Text("Gateway note") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    vm.saveConfig(
                        ConfigPatch(
                            adIntervalSeconds = adInterval.toIntOrNull(),
                            bannerEnabled = if (banner) 1 else 0,
                            interstitialEnabled = if (interstitial) 1 else 0,
                            freeAdMultiplier = multiplier.toIntOrNull(),
                            subscriptionPriceToman = price.toIntOrNull(),
                            subscriptionDurationDays = duration.toIntOrNull(),
                            cardNumber = cardNumber,
                            cardHolder = cardHolder,
                            cryptoWallet = cryptoWallet,
                            cryptoNetwork = cryptoNetwork,
                            gatewayEnabled = 0,
                            gatewayStatusNote = gatewayNote.ifBlank { "coming soon" }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save config")
            }
        }
    }
}
