package com.toolbox.pro.monetization.admob

import android.app.Activity
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun loadInterstitialAd() {
        // TODO: Implement AdMob when Google Maven is accessible
    }

    fun showInterstitialAd(activity: Activity, onDismissed: () -> Unit = {}) {
        onDismissed()
    }

    fun loadRewardedAd() {
        // TODO: Implement AdMob when Google Maven is accessible
    }

    fun showRewardedAd(activity: Activity, onReward: () -> Unit) {
        onReward()
    }
}
