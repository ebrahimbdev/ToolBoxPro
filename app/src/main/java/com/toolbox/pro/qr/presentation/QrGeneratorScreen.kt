package com.toolbox.pro.qr.presentation

import android.Manifest
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toolbox.pro.core.localization.LocalStrings
import com.toolbox.pro.qr.data.ContactsRepository
import com.toolbox.pro.qr.data.WifiNetwork
import com.toolbox.pro.qr.domain.BorderWidth
import com.toolbox.pro.qr.domain.DotStyle
import com.toolbox.pro.qr.domain.LogoPosition
import com.toolbox.pro.ui.components.ToolHeader

private val QrPalette = listOf(
    0xFF000000, 0xFF37474F, 0xFF6C63FF, 0xFF9C27B0, 0xFFE91E63, 0xFFF44336,
    0xFFE65100, 0xFFFFB300, 0xFF2E7D32, 0xFF00897B, 0xFF0277BD, 0xFF4E342E
)

private val BorderPalette = listOf(
    0xFF000000, 0xFF6C63FF, 0xFF9C27B0, 0xFFF44336, 0xFF00897B, 0xFF0277BD
)

private val BackgroundPalette = listOf(
    0xFFFFFFFF, 0xFFF5F7FB, 0xFFFFF6E5, 0xFFEAF7F0, 0xFFFDEFF3
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGeneratorScreen(
    viewModel: QrViewModel = hiltViewModel()
) {
    val s = LocalStrings.current
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var expandedContentType by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.onLogoSelected(context, it) } }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onContactsPermissionResult(granted) }

    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSaveMessage()
        }
    }

    Scaffold(
        topBar = {
            ToolHeader(
                title = s.qrGenerator,
                icon = Icons.Filled.QrCode,
                tabs = listOf(s.tabContent, s.tabDesign),
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.onSelectedTabChange(it) }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Fixed preview so style edits stay visible while controls scroll.
            PreviewCard(
                uiState = uiState,
                onSave = { viewModel.saveQrToGallery(context) },
                onShare = { viewModel.shareQrImage(context) }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.selectedTab == 0) {
                    ContentTypeCard(
                        uiState = uiState,
                        expanded = expandedContentType,
                        onExpandedChange = { expandedContentType = it },
                        onTypeSelected = { type ->
                            viewModel.onContentTypeChange(type)
                            expandedContentType = false
                        },
                        urlText = s.url,
                        textLabel = s.text,
                        wifiLabel = s.wifi,
                        contactLabel = s.contact,
                        typeTitle = s.contentType
                    )

                    when (uiState.contentType) {
                        "WIFI" -> WifiPanel(uiState = uiState, viewModel = viewModel)
                        "CONTACT" -> ContactsPanel(
                            uiState = uiState,
                            viewModel = viewModel,
                            onRequestPermission = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                        )
                        else -> TextContentPanel(uiState = uiState, viewModel = viewModel)
                    }
                } else {
                    DesignPanel(
                        uiState = uiState,
                        viewModel = viewModel,
                        onPickLogo = { logoPickerLauncher.launch("image/*") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PreviewCard(
    uiState: QrUiState,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val s = LocalStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isGenerating -> CircularProgressIndicator()

                uiState.qrBitmap != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        bitmap = uiState.qrBitmap!!.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(196.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (uiState.isStale) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .padding(6.dp),
                        contentScale = ContentScale.Fit,
                        alpha = if (uiState.isStale) 0.5f else 1f
                    )
                    if (uiState.isStale) {
                        Text(
                            s.contentChanged,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onSave, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.saveImage)
                        }
                        FilledTonalButton(onClick = onShare, shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(s.share)
                        }
                    }
                }

                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.QrCode, null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        s.enterToGenerate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentTypeCard(
    uiState: QrUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTypeSelected: (String) -> Unit,
    urlText: String,
    textLabel: String,
    wifiLabel: String,
    contactLabel: String,
    typeTitle: String
) {
    val types = listOf("URL" to urlText, "TEXT" to textLabel, "WIFI" to wifiLabel, "CONTACT" to contactLabel)
    val currentLabel = types.firstOrNull { it.first == uiState.contentType }?.second ?: urlText

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                typeTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { onExpandedChange(!expanded) }
            ) {
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                    types.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onTypeSelected(code) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TextContentPanel(
    uiState: QrUiState,
    viewModel: QrViewModel
) {
    val s = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                s.content,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.content,
                onValueChange = { viewModel.onContentChange(it) },
                placeholder = { Text(s.enterContentHint) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.onGenerateClick() },
                enabled = uiState.content.isNotBlank() && !uiState.isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.QrCode, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(s.generateQr, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ------------------------------------------------------------------ wifi

@Composable
private fun WifiPanel(
    uiState: QrUiState,
    viewModel: QrViewModel
) {
    val s = LocalStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    s.wifiNetworks,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.wifiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = { viewModel.loadWifiNetworks() }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = s.refresh,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (uiState.wifiNetworks.isEmpty() && !uiState.wifiLoading) {
                Text(
                    s.wifiListLimited,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.wifiNetworks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    uiState.wifiNetworks.forEach { network ->
                        WifiRow(
                            network = network,
                            selected = uiState.selectedWifi?.ssid == network.ssid,
                            onClick = { viewModel.onWifiSelected(network) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.manualSsid,
                    onValueChange = { viewModel.onManualSsidChange(it) },
                    placeholder = { Text(s.enterSsid) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = { viewModel.onAddManualWifi() },
                    enabled = uiState.manualSsid.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(s.addNetwork)
                }
            }

            uiState.selectedWifi?.let { network ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        network.ssid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OptionGrid(
                        options = listOf(
                            "WPA" to "WPA/WPA2/WPA3",
                            "WEP" to "WEP",
                            "nopass" to s.openNetwork
                        ),
                        selected = uiState.wifiSecurity,
                        onSelect = { viewModel.onWifiSecurityChange(it) },
                        columns = 3
                    )

                    if (uiState.wifiSecurity != "nopass") {
                        OutlinedTextField(
                            value = uiState.wifiPassword,
                            onValueChange = { viewModel.onWifiPasswordChange(it) },
                            label = { Text(s.wifiPassword) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { viewModel.onGenerateWifiClick() },
                        enabled = uiState.wifiSecurity == "nopass" || uiState.wifiPassword.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.QrCode, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(s.generateQr)
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiRow(
    network: WifiNetwork,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            network.ssid,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            when {
                network.isCurrent -> "●"
                network.security == "nopass" -> "○"
                else -> "·"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = content
        )
    }
}

// ------------------------------------------------------------- contacts

@Composable
private fun ContactsPanel(
    uiState: QrUiState,
    viewModel: QrViewModel,
    onRequestPermission: () -> Unit
) {
    val s = LocalStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                s.contactsTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!uiState.contactsPermissionGranted) {
                Text(s.contactsPermissionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    s.contactsPermissionDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(s.grantPermission)
                }
            } else {
                OutlinedTextField(
                    value = uiState.contactsSearch,
                    onValueChange = { viewModel.onContactsSearchChange(it) },
                    placeholder = { Text(s.searchContacts) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                when {
                    uiState.contactsLoading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    uiState.contacts.isEmpty() -> Text(
                        s.noContacts,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    else -> Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 230.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val atLimit = uiState.selectedContacts.size >= ContactsRepository.MAX_CONTACTS
                        uiState.filteredContacts.forEach { contact ->
                            val selected = contact.id in uiState.selectedContactIds
                            ContactRow(
                                name = contact.name,
                                numbers = contact.numbers,
                                selected = selected,
                                enabled = selected || !atLimit,
                                onToggle = { viewModel.onContactToggle(contact.id) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        String.format(s.contactsSelected, uiState.selectedContactIds.size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        s.contactsLimit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.onGenerateContactsClick() },
                    enabled = uiState.selectedContacts.isNotEmpty() &&
                        !uiState.contactsLimitExceeded &&
                        !uiState.contactsLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.QrCode, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(s.generateQr)
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    name: String,
    numbers: List<String>,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .clickable(enabled = enabled) { onToggle() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() }, enabled = enabled)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            numbers.forEach { number ->
                Text(
                    number,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ---------------------------------------------------------------- design

@Composable
private fun DesignPanel(
    uiState: QrUiState,
    viewModel: QrViewModel,
    onPickLogo: () -> Unit
) {
    val s = LocalStrings.current
    val style = uiState.style
    var customDialog by remember { mutableStateOf<String?>(null) }

    SectionCard(title = s.qrColor) {
        ColorSwatchRow(
            palette = QrPalette,
            selected = style.primaryColor,
            onSelect = { color -> viewModel.onStyleChange { it.copy(primaryColor = color) } },
            onCustom = { customDialog = "primary" }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(s.gradient, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Switch(
                checked = style.useGradient,
                onCheckedChange = { checked -> viewModel.onStyleChange { it.copy(useGradient = checked) } }
            )
        }
        if (style.useGradient) {
            Spacer(modifier = Modifier.height(8.dp))
            ColorSwatchRow(
                palette = QrPalette,
                selected = style.secondaryColor,
                onSelect = { color -> viewModel.onStyleChange { it.copy(secondaryColor = color) } },
                onCustom = { customDialog = "secondary" }
            )
        }
    }

    SectionCard(title = s.dotStyle) {
        OptionGrid(
            options = listOf(
                DotStyle.SQUARE to s.squareDots,
                DotStyle.ROUNDED to s.roundDots
            ),
            selected = style.dotStyle,
            onSelect = { value -> viewModel.onStyleChange { it.copy(dotStyle = value) } },
            columns = 2
        )
    }

    SectionCard(title = s.border) {
        OptionGrid(
            options = listOf(
                BorderWidth.NONE to s.borderNone,
                BorderWidth.THIN to s.borderThin,
                BorderWidth.MEDIUM to s.borderMedium,
                BorderWidth.THICK to s.borderThick
            ),
            selected = style.border,
            onSelect = { value -> viewModel.onStyleChange { it.copy(border = value) } },
            columns = 4
        )
        if (style.border != BorderWidth.NONE) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                s.borderColor,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            ColorSwatchRow(
                palette = BorderPalette,
                selected = style.borderColor,
                onSelect = { color -> viewModel.onStyleChange { it.copy(borderColor = color) } },
                onCustom = { customDialog = "border" }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.cornerRadius, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${style.cornerRadiusDp.toInt()}", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = style.cornerRadiusDp,
                onValueChange = { value -> viewModel.onStyleChange { it.copy(cornerRadiusDp = value) } },
                valueRange = 0f..48f
            )
        }
    }

    SectionCard(title = s.background) {
        ColorSwatchRow(
            palette = BackgroundPalette,
            selected = style.backgroundColor,
            onSelect = { color -> viewModel.onStyleChange { it.copy(backgroundColor = color) } },
            onCustom = null
        )
    }

    SectionCard(title = s.logo) {
        val logoPicked = uiState.logoBitmap != null
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (logoPicked) {
                uiState.logoBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            FilledTonalButton(
                onClick = onPickLogo,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (logoPicked) s.changeLogo else s.addLogo)
            }
            if (logoPicked) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.onLogoRemoved() }) {
                    Icon(Icons.Filled.Close, s.removeLogo, tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (logoPicked) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.logoSize, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${(style.logoSizeFraction * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = style.logoSizeFraction,
                onValueChange = { value -> viewModel.onStyleChange { it.copy(logoSizeFraction = value) } },
                valueRange = 0.10f..0.30f
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                s.logoPosition,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OptionGrid(
                options = listOf(
                    LogoPosition.CENTER to s.posCenter,
                    LogoPosition.TOP_LEFT to s.posTopLeft,
                    LogoPosition.TOP_RIGHT to s.posTopRight,
                    LogoPosition.BOTTOM_LEFT to s.posBottomLeft,
                    LogoPosition.BOTTOM_RIGHT to s.posBottomRight,
                    LogoPosition.NONE to s.posNone
                ),
                selected = style.logoPosition,
                onSelect = { value -> viewModel.onStyleChange { it.copy(logoPosition = value) } },
                columns = 3
            )
        }
    }

    customDialog?.let { target ->
        val initial = when (target) {
            "primary" -> style.primaryColor
            "secondary" -> style.secondaryColor
            else -> style.borderColor
        }
        CustomColorDialog(
            initial = initial,
            onCancel = { customDialog = null },
            onConfirm = { color ->
                viewModel.onStyleChange {
                    when (target) {
                        "primary" -> it.copy(primaryColor = color)
                        "secondary" -> it.copy(secondaryColor = color)
                        else -> it.copy(borderColor = color)
                    }
                }
                customDialog = null
            }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ColorSwatchRow(
    palette: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit,
    onCustom: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        palette.forEach { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .then(
                        if (isSelected) Modifier.border(
                            width = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ) else Modifier
                    )
                    .clickable { onSelect(color) }
            )
        }
        if (onCustom != null) {
            Text(
                "+",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onCustom() }
                    .padding(6.dp)
            )
        }
    }
}

@Composable
private fun <T> OptionGrid(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    columns: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    val isSelected = value == selected
                    OutlinedButton(
                        onClick = { onSelect(value) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                repeat((columns - row.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CustomColorDialog(
    initial: Long,
    onCancel: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val s = LocalStrings.current
    var red by remember { mutableStateOf(((initial shr 16) and 0xFF).toFloat()) }
    var green by remember { mutableStateOf(((initial shr 8) and 0xFF).toFloat()) }
    var blue by remember { mutableStateOf((initial and 0xFF).toFloat()) }
    val preview = AndroidColor.rgb(red.toInt(), green.toInt(), blue.toInt()).toLong() and 0xFFFFFFFFL

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(s.customColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(preview))
                )
                LabeledSlider("R", red, 0f..255f) { red = it }
                LabeledSlider("G", green, 0f..255f) { green = it }
                LabeledSlider("B", blue, 0f..255f) { blue = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(preview) }) { Text(s.done) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(s.cancel) }
        }
    )
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(20.dp), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
        Text(
            value.toInt().toString(),
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
