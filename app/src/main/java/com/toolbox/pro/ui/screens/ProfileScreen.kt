package com.toolbox.pro.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.toolbox.pro.core.config.RemoteConfigRepository
import com.toolbox.pro.core.identity.IdentityEntryPoint
import com.toolbox.pro.core.localization.AppLanguage
import com.toolbox.pro.core.localization.LanguageEntryPoint
import com.toolbox.pro.core.localization.LocalStrings
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController? = null) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var usernameDraft by remember { mutableStateOf("") }

    val languageRepo = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(context.applicationContext, LanguageEntryPoint::class.java)
                .languageRepository()
        }.getOrNull()
    }
    val identityEntry = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(context.applicationContext, IdentityEntryPoint::class.java)
        }.getOrNull()
    }
    val remoteConfig = remember(context) {
        identityEntry?.remoteConfigRepository()
    }
    val config by if (remoteConfig != null) {
        remoteConfig.config.collectAsState()
    } else {
        remember { mutableStateOf(com.toolbox.pro.core.api.RemoteConfigDto()) }
    }
    val username by if (identityEntry != null) {
        identityEntry.identityStore().username.collectAsState(initial = "")
    } else {
        remember { mutableStateOf("") }
    }
    val currentLang by if (languageRepo != null) {
        languageRepo.language.collectAsState(initial = "en")
    } else {
        remember { androidx.compose.runtime.mutableStateOf("en") }
    }

    LaunchedEffect(remoteConfig) { remoteConfig?.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.profile, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(
                        brush = Brush.linearGradient(colors = listOf(Color(0xFF6C63FF), Color(0xFF9C27B0)))
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Star, null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    username.ifBlank { s.freeTrialAds },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    if (config.subscriptionPriceToman > 0) {
                                        "${config.subscriptionPriceToman} Toman · ${config.subscriptionDurationDays}d"
                                    } else {
                                        "ToolBox Pro"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(s.freeTrialAds, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }

            SettingsItem(
                icon = Icons.Filled.Person,
                title = s.username,
                subtitle = username.ifBlank { s.chooseUsername },
                onClick = {
                    usernameDraft = username
                    showUsernameDialog = true
                }
            )

            SettingsItem(
                icon = Icons.Filled.Language,
                title = s.language,
                subtitle = AppLanguage.entries.find { it.code == currentLang }?.displayName ?: "English",
                onClick = { showLanguageDialog = true }
            )

            SettingsItem(icon = Icons.Filled.Palette, title = s.appearanceSettings, subtitle = s.darkModeColors)
            SettingsItem(icon = Icons.Filled.Info, title = s.about, subtitle = "ToolBox Pro")
        }
    }

    if (showUsernameDialog && identityEntry != null) {
        val store = identityEntry.identityStore()
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text(s.editUsername) },
            text = {
                OutlinedTextField(
                    value = usernameDraft,
                    onValueChange = { usernameDraft = it.take(64) },
                    label = { Text(s.username) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = usernameDraft.isNotBlank(),
                    onClick = {
                        scope.launch {
                            store.setUsername(usernameDraft)
                            remoteConfig?.registerAndHeartbeat()
                            showUsernameDialog = false
                        }
                    }
                ) { Text(s.done) }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameDialog = false }) { Text(s.done) }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(s.language) },
            text = {
                Column {
                    AppLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (languageRepo != null) {
                                    scope.launch {
                                        languageRepo.setLanguage(lang.code)
                                        showLanguageDialog = false
                                    }
                                } else {
                                    showLanguageDialog = false
                                }
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(s.done) }
            }
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, title, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
