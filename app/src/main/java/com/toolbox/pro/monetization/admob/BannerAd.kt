package com.toolbox.pro.monetization.admob

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.toolbox.pro.BuildConfig
import com.toolbox.pro.core.config.RemoteConfigRepository
import com.toolbox.pro.core.identity.IdentityEntryPoint
import dagger.hilt.android.EntryPointAccessors

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (LocalInspectionMode.current) return

    val remoteConfig = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                IdentityEntryPoint::class.java
            ).remoteConfigRepository()
        }.getOrNull()
    }
    val enabled = remoteConfig?.config?.value?.bannerEnabled ?: true
    if (!enabled) return

    DisposableEffect(Unit) {
        onDispose { }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
                loadAd(AdRequest.Builder().build())
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = { adView ->
            // reload if needed
        }
    )
}
