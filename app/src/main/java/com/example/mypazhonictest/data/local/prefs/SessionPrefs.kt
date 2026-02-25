package com.example.mypazhonictest.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "session_prefs")

class SessionPrefs(private val context: Context) {

    companion object {
        private val KEY_SESSION_TOKEN = stringPreferencesKey("session_token")
        private val KEY_USER_ID = longPreferencesKey("user_id")
    }

    val sessionTokenFlow: Flow<String?> =
        context.sessionDataStore.data.map { it[KEY_SESSION_TOKEN] }

    suspend fun setSession(token: String, userId: Long) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_SESSION_TOKEN] = token
            prefs[KEY_USER_ID] = userId
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.clear() }
    }

    suspend fun getSessionToken(): String? =
        context.sessionDataStore.data.map { it[KEY_SESSION_TOKEN] }.first()

    suspend fun getUserId(): Long? =
        context.sessionDataStore.data.map { it[KEY_USER_ID] }.first()
}
