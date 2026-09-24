package com.toolbox.admin.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.toolbox.admin.data.api.AdminApi
import com.toolbox.admin.data.api.AdminApiResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.adminStore: androidx.datastore.core.DataStore<
    androidx.datastore.preferences.core.Preferences
    > by preferencesDataStore(name = "admin_auth")

@Singleton
class AdminSession @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AdminApi
) {
    companion object {
        val TOKEN = stringPreferencesKey("admin_token")
    }

    val token: Flow<String> = context.adminStore.data.map { it[TOKEN] ?: "" }

    suspend fun restore(): Boolean {
        val saved = context.adminStore.data.first()[TOKEN] ?: ""
        if (saved.isBlank()) return false
        return when (api.login(saved)) {
            is AdminApiResult.Success -> true
            is AdminApiResult.Failure -> false
        }
    }

    suspend fun login(input: String): AdminApiResult<Boolean> {
        val result = api.login(input)
        if (result is AdminApiResult.Success) {
            context.adminStore.edit { it[TOKEN] = input.trim() }
        }
        return result
    }

    suspend fun logout() {
        context.adminStore.edit { it.remove(TOKEN) }
        api.setToken("")
    }
}
