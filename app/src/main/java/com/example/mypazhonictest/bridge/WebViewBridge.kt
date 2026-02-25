package com.example.mypazhonictest.bridge

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import com.example.mypazhonictest.data.local.entity.UserEntity
import com.example.mypazhonictest.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/**
 * JavaScript bridge exposed to the WebView as "AndroidBridge".
 * React can call: AndroidBridge.onReactReady(), AndroidBridge.registerUser(json), etc.
 */
class WebViewBridge(
    private val userRepository: UserRepository,
    private val biometricPrefs: com.example.mypazhonictest.data.local.prefs.BiometricPrefs,
    private val mainHandler: Handler,
    private val onReactReady: () -> Unit
) {

    @JavascriptInterface
    fun onReactReady() {
        Log.d(TAG, "onReactReady called from React")
        mainHandler.post(onReactReady)
    }

    /** Register or update user. Pass JSON with userName, phoneNumber, password (required); optional: fullName, firstName, lastName, nationalCode, avatarUrl, ipAddress. */
    @JavascriptInterface
    fun registerUser(userJson: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val obj = JSONObject(userJson)
                val userName = obj.optString("userName").takeIf { it.isNotEmpty() }
                    ?: return@runBlocking error("userName is required")
                val phoneNumber = obj.optString("phoneNumber").takeIf { it.isNotEmpty() }
                    ?: return@runBlocking error("phoneNumber is required")
                val password = obj.optString("password").takeIf { it.isNotEmpty() }
                    ?: return@runBlocking error("password is required")
                val user = UserEntity(
                    fullName = obj.optString("fullName").takeIf { it.isNotEmpty() },
                    firstName = obj.optString("firstName").takeIf { it.isNotEmpty() },
                    lastName = obj.optString("lastName").takeIf { it.isNotEmpty() },
                    userName = userName,
                    phoneNumber = phoneNumber,
                    nationalCode = obj.optString("nationalCode").takeIf { it.isNotEmpty() },
                    avatarUrl = obj.optString("avatarUrl").takeIf { it.isNotEmpty() },
                    ipAddress = obj.optString("ipAddress").takeIf { it.isNotEmpty() },
                    password = password
                )
                userRepository.registerLocalUser(user)
                """{"success":true}"""
            } catch (e: Exception) {
                Log.e(TAG, "registerUser error", e)
                """{"success":false,"error":"${e.message?.replace("\"", "'")}"}"""
            }
        }
    }

    /** Login with phone and password. Returns JSON: { success, token?, user? } or { success: false, error }. Token is stored for session. */
    @JavascriptInterface
    fun login(phoneNumber: String, password: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val result = userRepository.loginByPhoneAndPassword(phoneNumber, password)
                if (result != null) {
                    """{"success":true,"token":"${escapeJson(result.token)}","user":${userToJson(result.user)}}"""
                } else {
                    """{"success":false,"error":"Invalid phone or password"}"""
                }
            } catch (e: Exception) {
                Log.e(TAG, "login error", e)
                """{"success":false,"error":"${e.message?.replace("\"", "'")}"}"""
            }
        }
    }

    /** Get current session token or null. Returns JSON: { "token": "..." } or { "token": null }. */
    @JavascriptInterface
    fun getSessionToken(): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val token = userRepository.getSessionToken()
                if (token != null) """{"token":"${escapeJson(token)}"}""" else """{"token":null}"""
            } catch (e: Exception) {
                Log.e(TAG, "getSessionToken error", e)
                """{"token":null,"error":"${e.message?.replace("\"", "'")}"}"""
            }
        }
    }

    /** Get current user JSON or {"user":null}. Returns user only if session is valid (logged in). */
    @JavascriptInterface
    fun getLatestUser(): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val user = userRepository.getLatestUser()
                if (user != null) """{"user":${userToJson(user)}}""" else """{"user":null}"""
            } catch (e: Exception) {
                Log.e(TAG, "getLatestUser error", e)
                """{"user":null,"error":"${e.message?.replace("\"", "'")}"}"""
            }
        }
    }

    @JavascriptInterface
    fun logout() {
        runBlocking(Dispatchers.IO) {
            try {
                userRepository.logoutLocal()
            } catch (e: Exception) {
                Log.e(TAG, "logout error", e)
            }
        }
    }

    @JavascriptInterface
    fun setBiometricEnabled(enabled: String) {
        runBlocking(Dispatchers.IO) {
            try {
                biometricPrefs.setBiometricEnabled(enabled.equals("true", ignoreCase = true))
            } catch (e: Exception) {
                Log.e(TAG, "setBiometricEnabled error", e)
            }
        }
    }

    @JavascriptInterface
    fun getBiometricEnabled(): String {
        return runBlocking(Dispatchers.IO) {
            try {
                biometricPrefs.biometricEnabledFlow.first().toString()
            } catch (e: Exception) {
                Log.e(TAG, "getBiometricEnabled error", e)
                "false"
            }
        }
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")

    private fun userToJson(user: UserEntity): String {
        val o = JSONObject()
        o.put("id", user.id)
        o.put("userName", user.userName)
        o.put("phoneNumber", user.phoneNumber)
        o.put("fullName", user.fullName)
        o.put("firstName", user.firstName)
        o.put("lastName", user.lastName)
        o.put("nationalCode", user.nationalCode)
        o.put("avatarUrl", user.avatarUrl)
        o.put("ipAddress", user.ipAddress)
        o.put("createdAt", user.createdAt)
        o.put("updatedAt", user.updatedAt)
        return o.toString()
    }

    companion object {
        private const val TAG = "WebViewBridge"
    }
}
