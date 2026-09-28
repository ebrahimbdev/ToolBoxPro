package com.toolbox.pro.core.config

import com.toolbox.pro.core.api.ApiResult
import com.toolbox.pro.core.api.AppApi
import com.toolbox.pro.core.api.RemoteConfigDto
import com.toolbox.pro.core.identity.DeviceIdentityStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigRepository @Inject constructor(
    private val api: AppApi,
    private val identityStore: DeviceIdentityStore
) {
    private val _config = MutableStateFlow(RemoteConfigDto())
    val config: StateFlow<RemoteConfigDto> = _config.asStateFlow()

    private val _synced = MutableStateFlow(false)
    val synced: StateFlow<Boolean> = _synced.asStateFlow()

    suspend fun refresh() {
        val identity = identityStore.snapshot()
        when (val result = api.getConfig(identity)) {
            is ApiResult.Success -> {
                _config.value = result.data
                _synced.value = true
            }
            is ApiResult.Failure -> {
                // keep defaults
            }
        }
    }

    suspend fun registerAndHeartbeat(adViews: Int = 0, toolOpens: Int = 0) {
        val identity = identityStore.snapshot()
        when (api.register(identity)) {
            is ApiResult.Success -> {
                api.heartbeat(identity, adViews = adViews, toolOpens = toolOpens)
                refresh()
            }
            is ApiResult.Failure -> {
                api.heartbeat(identity, adViews = adViews, toolOpens = toolOpens)
                refresh()
            }
        }
    }

    /**
     * Full startup sync used by the connection screen: registers the device,
     * heartbeats and pulls the remote config, then classifies the failure so
     * the UI can show 409 (server unreachable) vs 410 (not registered).
     */
    suspend fun sync(): SyncStatus {
        val identity = identityStore.snapshot()
        return when (val reg = api.register(identity)) {
            is ApiResult.Failure -> reg.toStatus("register")
            is ApiResult.Success -> {
                api.heartbeat(identity)
                when (val cfg = api.getConfig(identity)) {
                    is ApiResult.Failure -> cfg.toStatus("config")
                    is ApiResult.Success -> {
                        _config.value = cfg.data
                        _synced.value = true
                        SyncStatus(SyncOutcome.OK)
                    }
                }
            }
        }
    }

    /**
     * Lightweight foreground heartbeat so the admin dashboard can see devices
     * as online in near real time (updates `last_seen` every minute).
     */
    suspend fun ping() {
        val identity = identityStore.snapshot()
        api.heartbeat(identity)
    }

    private fun ApiResult.Failure.toStatus(step: String): SyncStatus {
        val outcome =
            if (code == 0 || code >= 500) SyncOutcome.SERVER_DOWN else SyncOutcome.NOT_REGISTERED
        val reason = if (code == 0) message else "HTTP $code: $message"
        return SyncStatus(outcome, "$step -> $reason")
    }

    data class SyncStatus(val outcome: SyncOutcome, val detail: String = "")

    enum class SyncOutcome { OK, SERVER_DOWN, NOT_REGISTERED }
}
