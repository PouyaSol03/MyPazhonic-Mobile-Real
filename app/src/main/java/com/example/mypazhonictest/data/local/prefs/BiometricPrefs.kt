package com.example.mypazhonictest.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

class BiometricPrefs(private val context: Context) {

    companion object {
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_BIOMETRIC_DIALOG_SHOWN = booleanPreferencesKey("biometric_dialog_shown")
    }

    val biometricEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }

    val biometricDialogShownFlow: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_BIOMETRIC_DIALOG_SHOWN] ?: false }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setBiometricDialogShown(shown: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_DIALOG_SHOWN] = shown
        }
    }
}
