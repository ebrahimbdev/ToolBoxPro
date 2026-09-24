package com.toolbox.pro.core.localization

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.langDataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(name = "app_language")

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LanguageEntryPoint {
    fun languageRepository(): LanguageRepository
}

@Singleton
class LanguageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
    }

    val language: Flow<String> = context.langDataStore.data
        .map { it[LANGUAGE_KEY] ?: "en" }

    suspend fun setLanguage(lang: String) {
        context.langDataStore.edit { it[LANGUAGE_KEY] = lang }
    }
}

@Composable
fun LanguageProvider(
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(context.applicationContext, LanguageEntryPoint::class.java)
                .languageRepository()
        }.getOrNull()
    }
    val lang by if (repo != null) {
        repo.language.collectAsState(initial = "en")
    } else {
        remember { androidx.compose.runtime.mutableStateOf("en") }
    }

    CompositionLocalProvider(LocalStrings provides stringsFor(lang)) {
        content()
    }
}
