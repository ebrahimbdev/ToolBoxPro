package com.toolbox.pro.core.identity

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.toolbox.pro.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.identityStore: androidx.datastore.core.DataStore<
    androidx.datastore.preferences.core.Preferences
    > by preferencesDataStore(name = "device_identity")

data class DeviceIdentity(
    val deviceId: String,
    val username: String,
    val brand: String,
    val model: String,
    val osVersion: String,
    val appVersion: String
)

@Singleton
class DeviceIdentityStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val USERNAME = stringPreferencesKey("username")
    }

    val deviceId: Flow<String> = context.identityStore.data.map { prefs ->
        prefs[DEVICE_ID] ?: ""
    }

    val username: Flow<String> = context.identityStore.data.map { prefs ->
        prefs[USERNAME] ?: ""
    }

    suspend fun ensureDeviceId(): String {
        val existing = context.identityStore.data.first()[DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        context.identityStore.edit { it[DEVICE_ID] = generated }
        return generated
    }

    suspend fun setUsername(value: String) {
        context.identityStore.edit { it[USERNAME] = value.trim().take(64) }
    }

    suspend fun snapshot(): DeviceIdentity {
        val id = ensureDeviceId()
        val name = context.identityStore.data.first()[USERNAME] ?: ""
        return DeviceIdentity(
            deviceId = id,
            username = name,
            brand = Build.BRAND.orEmpty(),
            model = Build.MODEL.orEmpty(),
            osVersion = Build.VERSION.RELEASE.orEmpty(),
            appVersion = BuildConfig.VERSION_NAME
        )
    }
}
