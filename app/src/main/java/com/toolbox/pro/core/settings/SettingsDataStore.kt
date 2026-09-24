package com.toolbox.pro.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
}
