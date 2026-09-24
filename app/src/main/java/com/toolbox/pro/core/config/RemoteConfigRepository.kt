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
}
