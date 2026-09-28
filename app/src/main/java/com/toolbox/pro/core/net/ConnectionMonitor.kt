package com.toolbox.pro.core.net

import android.util.Log
import com.toolbox.pro.core.config.RemoteConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/** Blocking error reasons shown to the user with their display codes. */
sealed class ConnIssue(val code: Int) {
    data object Server : ConnIssue(409)          // cannot reach the server
    data object NotRegistered : ConnIssue(410)   // could not connect/register in the admin app
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ConnectionMonitorEntryPoint {
    fun connectionMonitor(): ConnectionMonitor
}

/**
 * Startup connectivity gate: classifies API failures so the UI can block with
 * 409 (server down) or 410 (not registered in the admin app).
 */
@Singleton
class ConnectionMonitor @Inject constructor(
    private val remoteConfig: RemoteConfigRepository
) {
    private val _issue = MutableStateFlow<ConnIssue?>(null)
    val issue: StateFlow<ConnIssue?> = _issue.asStateFlow()

    /** Human-readable reason behind the last failure (shown on the error screen). */
    private val _detail = MutableStateFlow("")
    val detail: StateFlow<String> = _detail.asStateFlow()

    suspend fun recheck(): ConnIssue? {
        var found: ConnIssue?
        var detail: String
        try {
            val status = remoteConfig.sync()
            found = when (status.outcome) {
                RemoteConfigRepository.SyncOutcome.OK -> null
                RemoteConfigRepository.SyncOutcome.SERVER_DOWN -> ConnIssue.Server
                RemoteConfigRepository.SyncOutcome.NOT_REGISTERED -> ConnIssue.NotRegistered
            }
            detail = if (found == null) "" else status.detail
        } catch (e: Exception) {
            found = ConnIssue.Server
            detail = "exception -> ${e.javaClass.simpleName}: ${e.message}"
        }
        if (found != null) Log.w(TAG, "sync failed [$detail]")
        else Log.i(TAG, "sync ok")
        _detail.value = detail
        _issue.value = found
        return found
    }

    fun clear() {
        _detail.value = ""
        _issue.value = null
    }

    private companion object {
        const val TAG = "ToolBoxNet"
    }
}
