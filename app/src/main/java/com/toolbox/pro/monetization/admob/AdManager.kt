package com.toolbox.pro.monetization.admob

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.toolbox.pro.BuildConfig
import com.toolbox.pro.core.config.RemoteConfigRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdEntryPoint {
    fun adManager(): AdManager
}

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfig: RemoteConfigRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var interstitialAd: InterstitialAd? = null
    private var lastShowAtMs: Long = 0L
    private var pendingCallback: (() -> Unit)? = null

    fun init() {
        // MobileAds.initialize is called lazily on first load
        loadInterstitialAd()
    }

    fun loadInterstitialAd() {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadInterstitialAd()
                            pendingCallback?.invoke()
                            pendingCallback = null
                        }

                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            interstitialAd = null
                            loadInterstitialAd()
                            pendingCallback?.invoke()
                            pendingCallback = null
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun intervalMs(): Long {
        val seconds = remoteConfig.config.value.adIntervalSeconds.coerceIn(5, 600)
        return seconds * 1000L
    }

    private fun interstitialEnabled(): Boolean {
        return remoteConfig.config.value.interstitialEnabled
    }

    fun maybeShowInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        if (!interstitialEnabled()) {
            onDismissed()
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastShowAtMs < intervalMs()) {
            onDismissed()
            return
        }
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitialAd()
            onDismissed()
            return
        }
        pendingCallback = onDismissed
        lastShowAtMs = now
        ad.show(activity)
    }

    fun showInterstitialAd(activity: Activity, onDismissed: () -> Unit = {}) {
        maybeShowInterstitial(activity, onDismissed)
    }

    fun loadRewardedAd() {
        // Rewarded ads not required for free-trial-with-ads plan
    }

    fun showRewardedAd(activity: Activity, onReward: () -> Unit) {
        onReward()
    }

    fun trackAdView() {
        scope.launch {
            runCatching {
                remoteConfig.registerAndHeartbeat(adViews = 1)
            }
        }
    }
}
