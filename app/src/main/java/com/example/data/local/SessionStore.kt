package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.razakDataStore by preferencesDataStore(name = "razak_task_prefs")

class SessionStore(private val context: Context) {
    private val keyDarkTheme = booleanPreferencesKey("dark_theme_override")
    private val keyLanguageCode = stringPreferencesKey("preferred_language_code")
    private val keyOnboardingDone = booleanPreferencesKey("language_onboarding_done")

    val darkThemeFlow: Flow<Boolean?> = context.razakDataStore.data.map { prefs ->
        prefs[keyDarkTheme]
    }

    val languageCodeFlow: Flow<String> = context.razakDataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[keyLanguageCode]).code
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.razakDataStore.data.map { prefs ->
        prefs[keyOnboardingDone] ?: false
    }

    suspend fun setDarkTheme(enabled: Boolean?) {
        context.razakDataStore.edit { prefs ->
            if (enabled == null) {
                prefs.remove(keyDarkTheme)
            } else {
                prefs[keyDarkTheme] = enabled
            }
        }
    }

    suspend fun setLanguageCode(code: String) {
        val validCode = AppLanguage.fromCode(code).code
        context.razakDataStore.edit { prefs ->
            prefs[keyLanguageCode] = validCode
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.razakDataStore.edit { prefs ->
            prefs[keyOnboardingDone] = completed
        }
    }
}
