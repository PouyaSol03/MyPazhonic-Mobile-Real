package com.example.mypazhonictest.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores token + user JSON for biometric login. Encrypted at rest.
 * When user logs in with password and biometric is enabled, we save here.
 * When they tap "ورود با اثر انگشت", after BiometricPrompt success we read and restore session.
 */
class BiometricCredentialStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "biometric_credential_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokenAndUserJson(token: String, userJson: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_JSON, userJson)
            .apply()
    }

    fun getTokenAndUserJson(): Pair<String, String>? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val userJson = prefs.getString(KEY_USER_JSON, null) ?: return null
        if (token.isEmpty() || userJson.isEmpty()) return null
        return token to userJson
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasStoredCredentials(): Boolean = getTokenAndUserJson() != null

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_JSON = "user_json"
    }
}
