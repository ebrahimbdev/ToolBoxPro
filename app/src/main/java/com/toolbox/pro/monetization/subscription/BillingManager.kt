package com.toolbox.pro.monetization.subscription

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    private val application: Application
) {
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun checkPremiumStatus() {
        // TODO: Implement Google Play Billing when Google Maven is accessible
    }
}
