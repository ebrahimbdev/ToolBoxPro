package com.toolbox.pro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.toolbox.pro.core.net.ConnIssue
import com.toolbox.pro.core.net.ConnectionMonitorEntryPoint
import com.toolbox.pro.ui.screens.ConnectionErrorScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableStateFlow

private enum class Phase { Checking, Ready }

/**
 * Startup pipeline: sync with the server (blocking 409/410 screen on
 * failure), then let the app content in. The startup ad charge was removed
 * while ads are disabled — tools refill their free quota automatically.
 */
@Composable
fun AppEntry(content: @Composable () -> Unit) {
    val context = LocalContext.current

    val monitor = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ConnectionMonitorEntryPoint::class.java
            ).connectionMonitor()
        }.getOrNull()
    }

    var issue by remember { mutableStateOf<ConnIssue?>(null) }
    var phase by remember { mutableStateOf(Phase.Checking) }

    val emptyDetail = remember { MutableStateFlow("") }
    val detailFlow = monitor?.detail ?: emptyDetail
    val syncDetail by detailFlow.collectAsState()

    suspend fun runCheck() {
        val found = monitor?.recheck()
        issue = found
        if (found == null) phase = Phase.Ready
    }

    LaunchedEffect(Unit) { runCheck() }

    val currentIssue = issue
    when {
        currentIssue != null -> ConnectionErrorScreen(
            issue = currentIssue,
            detail = syncDetail,
            onRetry = { runCheck() }
        )

        phase == Phase.Ready -> content()

        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
