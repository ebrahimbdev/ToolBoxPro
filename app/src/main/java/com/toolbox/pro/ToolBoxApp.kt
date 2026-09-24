package com.toolbox.pro

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.toolbox.pro.monetization.admob.AdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ToolBoxApp : Application() {

    @Inject
    lateinit var adManager: AdManager

    override fun onCreate() {
        super.onCreate()
        Thread {
            runCatching { MobileAds.initialize(this) }
            runCatching { adManager.init() }
        }.start()
    }
}
