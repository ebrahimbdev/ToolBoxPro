package com.toolbox.pro.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AD_DISPLAY_INTERVAL = intPreferencesKey("ad_display_interval")
        val LANGUAGE = stringPreferencesKey("language")
        val USES_SINCE_AD = intPreferencesKey("uses_since_ad")
        val QUOTA_TOTAL = intPreferencesKey("quota_total")
        val QUOTA_LEFT = intPreferencesKey("quota_left")
        val QUOTA_GRANTED_AT = longPreferencesKey("quota_granted_at")
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_PREMIUM] ?: false
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }

    val adDisplayInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[AD_DISPLAY_INTERVAL] ?: 3
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE] ?: "fa"
    }

    val usesSinceAd: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[USES_SINCE_AD] ?: 0
    }

    val quotaTotal: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[QUOTA_TOTAL] ?: 0
    }

    val quotaLeft: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[QUOTA_LEFT] ?: 0
    }

    val quotaGrantedAt: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[QUOTA_GRANTED_AT] ?: 0L
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_PREMIUM] = isPremium
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }

    suspend fun setAdDisplayInterval(interval: Int) {
        context.dataStore.edit { prefs ->
            prefs[AD_DISPLAY_INTERVAL] = interval
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE] = lang
        }
    }

    suspend fun setUsesSinceAd(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[USES_SINCE_AD] = count.coerceAtLeast(0)
        }
    }

    suspend fun setQuota(total: Int, left: Int, grantedAt: Long) {
        context.dataStore.edit { prefs ->
            prefs[QUOTA_TOTAL] = total.coerceAtLeast(0)
            prefs[QUOTA_LEFT] = left.coerceIn(0, total.coerceAtLeast(0))
            prefs[QUOTA_GRANTED_AT] = grantedAt
        }
    }

    suspend fun setQuotaLeft(left: Int) {
        context.dataStore.edit { prefs ->
            val total = prefs[QUOTA_TOTAL] ?: 0
            prefs[QUOTA_LEFT] = left.coerceIn(0, total.coerceAtLeast(0))
        }
    }
}
