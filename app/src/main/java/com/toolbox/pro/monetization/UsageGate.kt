package com.toolbox.pro.monetization

import com.toolbox.pro.core.config.RemoteConfigRepository
import com.toolbox.pro.core.settings.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UsageGateEntryPoint {
    fun usageGate(): UsageGate
}

/**
 * Free-usage quota.
 *
 * - Ads are temporarily disabled: the grant refills itself automatically when
 *   it runs out (or expires), so tools never lock behind a missing ad.
 * - `total = 0` (admin config) or a premium user means no limit at all.
 * - Exposes reactive [quotaLeft]/[quotaTotal] so the header circle can render
 *   100 -> 0 as the free turns are consumed.
 */
@Singleton
class UsageGate @Inject constructor(
    private val settings: SettingsDataStore,
    private val remoteConfig: RemoteConfigRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _quotaLeft = MutableStateFlow(0)
    val quotaLeft: StateFlow<Int> = _quotaLeft.asStateFlow()

    private val _quotaTotal = MutableStateFlow(0)
    val quotaTotal: StateFlow<Int> = _quotaTotal.asStateFlow()

    private val _premium = MutableStateFlow(false)
    val premium: StateFlow<Boolean> = _premium.asStateFlow()

    init {
        scope.launch {
            settings.quotaLeft.collect { _quotaLeft.value = it }
        }
        scope.launch {
            settings.quotaTotal.collect { _quotaTotal.value = it }
        }
        scope.launch {
            settings.isPremium.collect { _premium.value = it }
        }
    }

    val limit: Int
        get() = remoteConfig.config.value.freeUsesPerAd.coerceIn(0, 100)

    val isUnlimited: Boolean
        get() = limit <= 0 || _premium.value

    /** Synchronous-startup helper for callers that gate on premium. */
    suspend fun isPremium(): Boolean = settings.isPremium.first()

    /** Remaining free turns as 100 -> 0 for the header circle. */
    val percentLeft: Int
        get() {
            val total = _quotaTotal.value
            if (total <= 0) return 100
            return (_quotaLeft.value * 100 / total).coerceIn(0, 100)
        }

    /** Charges a full grant (used for the automatic refill). */
    suspend fun grant() {
        val total = limit
        settings.setQuota(total, total, System.currentTimeMillis())
        _quotaTotal.value = total
        _quotaLeft.value = total
    }

    /** A grant is dead 4 hours after it was earned — it then refills itself. */
    suspend fun isExpired(): Boolean {
        val grantedAt = settings.quotaGrantedAt.first()
        if (grantedAt <= 0L) return true
        return System.currentTimeMillis() - grantedAt > QUOTA_TTL_MS
    }

    /**
     * Returns true when the tool may be opened. Decrements the quota on every
     * allowed entry and refills the free grant automatically when it is used
     * up or expired (ads disabled for now), so this never blocks the user.
     */
    suspend fun consume(): Boolean {
        if (isUnlimited) return true
        if (isExpired() || settings.quotaLeft.first() <= 0) grant()

        val left = settings.quotaLeft.first()
        if (left <= 0) return true
        settings.setQuotaLeft(left - 1)
        _quotaLeft.value = left - 1
        return true
    }

    companion object {
        const val QUOTA_TTL_MS = 4L * 60L * 60L * 1000L
    }
}
