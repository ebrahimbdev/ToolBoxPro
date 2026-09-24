package com.toolbox.pro.core.identity

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toolbox.pro.core.config.RemoteConfigRepository
import com.toolbox.pro.core.localization.LocalStrings
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface IdentityEntryPoint {
    fun identityStore(): DeviceIdentityStore
    fun remoteConfigRepository(): RemoteConfigRepository
}

@Composable
fun UsernameGate(content: @Composable () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    var showPrompt by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    val store = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                IdentityEntryPoint::class.java
            ).identityStore()
        }.getOrNull()
    }
    val remoteConfig = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                IdentityEntryPoint::class.java
            ).remoteConfigRepository()
        }.getOrNull()
    }
    val username by if (store != null) {
        store.username.collectAsState(initial = "")
    } else {
        remember { mutableStateOf("") }
    }

    LaunchedEffect(store) {
        if (store == null) {
            ready = true
        } else {
            store.ensureDeviceId()
            val existing = store.username
            val current = try {
                existing.first()
            } catch (_: Exception) {
                ""
            }
            if (current.isBlank()) {
                draft = ""
                showPrompt = true
            } else {
                ready = true
            }
            scope.launch {
                remoteConfig?.registerAndHeartbeat()
            }
        }
    }

    if (showPrompt && store != null) {
        AlertDialog(
            onDismissRequest = { /* require username once */ },
            title = { Text(s.chooseUsername) },
            text = {
                Column {
                    Text(s.usernameHint)
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(64) },
                        label = { Text(s.username) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = draft.isNotBlank(),
                    onClick = {
                        scope.launch {
                            store.setUsername(draft)
                            showPrompt = false
                            ready = true
                            remoteConfig?.registerAndHeartbeat()
                        }
                    }
                ) { Text(s.done) }
            }
        )
    }

    if (ready) {
        content()
    }
}
