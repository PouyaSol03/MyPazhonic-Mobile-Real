package com.example.mypazhonictest.bridge

import android.os.Handler
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.mypazhonictest.data.local.entity.UserEntity
import com.example.mypazhonictest.data.local.prefs.BiometricCredentialStore
import com.example.mypazhonictest.data.local.prefs.BiometricPrefs
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
    private val activity: FragmentActivity,
    private val webView: WebView,
    private val userRepository: UserRepository,
    private val biometricPrefs: BiometricPrefs,
    private val biometricCredentialStore: BiometricCredentialStore,
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
                    ?: return@runBlocking error("نام کاربری الزامی است")
                val phoneNumber = obj.optString("phoneNumber").takeIf { it.isNotEmpty() }
                    ?: return@runBlocking error("شماره موبایل الزامی است")
                val password = obj.optString("password").takeIf { it.isNotEmpty() }
                    ?: return@runBlocking error("رمز عبور الزامی است")
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
                val msg = e.message?.takeIf { it.isNotEmpty() }?.replace("\"", "'")
                    ?: "خطایی رخ داد. لطفا دوباره تلاش کنید."
                """{"success":false,"error":"$msg"}"""
            }
        }
    }

    /** Login with phone and password. Returns JSON: { success, token?, user? } or { success: false, error }. Token is stored for session. If biometric is enabled, stores credentials for biometric login. */
    @JavascriptInterface
    fun login(phoneNumber: String, password: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val result = userRepository.loginByPhoneAndPassword(phoneNumber, password)
                if (result != null) {
                    val biometricEnabled = biometricPrefs.biometricEnabledFlow.first()
                    if (biometricEnabled) {
                        biometricCredentialStore.saveTokenAndUserJson(
                            result.token,
                            userToJsonWithPassword(result.user)
                        )
                    }
                    """{"success":true,"token":"${escapeJson(result.token)}","user":${userToJson(result.user)}}"""
                } else {
                    """{"success":false,"error":"شماره موبایل یا رمز عبور اشتباه است"}"""
                }
            } catch (e: Exception) {
                Log.e(TAG, "login error", e)
                val msg = e.message?.takeIf { it.isNotEmpty() }?.replace("\"", "'")
                    ?: "خطایی رخ داد. لطفا دوباره تلاش کنید."
                """{"success":false,"error":"$msg"}"""
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
                val msg = e.message?.takeIf { it.isNotEmpty() }?.replace("\"", "'")
                    ?: "خطایی رخ داد. لطفا دوباره تلاش کنید."
                """{"token":null,"error":"$msg"}"""
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
                val msg = e.message?.takeIf { it.isNotEmpty() }?.replace("\"", "'")
                    ?: "خطایی رخ داد. لطفا دوباره تلاش کنید."
                """{"user":null,"error":"$msg"}"""
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
                val enable = enabled.equals("true", ignoreCase = true)
                biometricPrefs.setBiometricEnabled(enable)
                if (enable) {
                    // When user enables biometric while already logged in, save current session for biometric login
                    val token = userRepository.getSessionToken()
                    val user = userRepository.getLatestUser()
                    if (token != null && user != null) {
                        biometricCredentialStore.saveTokenAndUserJson(token, userToJsonWithPassword(user))
                    }
                } else {
                    biometricCredentialStore.clear()
                }
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

    /**
     * Start biometric login. Async: shows BiometricPrompt, then invokes JS callback with result JSON.
     * Callback receives one string argument: JSON like {"success":true,"token":"...","user":{...}} or {"success":false,"error":"..."}.
     */
    @JavascriptInterface
    fun loginWithBiometric(callbackJsName: String) {
        mainHandler.post {
            val biometricEnabled = runBlocking(Dispatchers.IO) {
                biometricPrefs.biometricEnabledFlow.first()
            }
            if (!biometricEnabled) {
                deliverBiometricResult(callbackJsName, """{"success":false,"error":"ورود با بیومتریک غیرفعال است"}""")
                return@post
            }
            if (!biometricCredentialStore.hasStoredCredentials()) {
                deliverBiometricResult(callbackJsName, """{"success":false,"error":"ابتدا با رمز عبور وارد شوید"}""")
                return@post
            }
            val biometricManager = BiometricManager.from(activity)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> { /* proceed */ }
                else -> {
                    deliverBiometricResult(callbackJsName, """{"success":false,"error":"احراز هویت بیومتریک در دسترس نیست"}""")
                    return@post
                }
            }
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val pair = runBlocking(Dispatchers.IO) {
                            val p = biometricCredentialStore.getTokenAndUserJson()
                            if (p != null) {
                                val user = jsonToUser(p.second)
                                if (user != null) {
                                    userRepository.restoreSessionAndUser(p.first, user)
                                    p.first to userToJson(user)
                                } else null
                            } else null
                        }
                        if (pair != null) {
                            val (token, userJson) = pair
                            val resultJson = """{"success":true,"token":"${escapeJson(token)}","user":$userJson}"""
                            mainHandler.post { deliverBiometricResult(callbackJsName, resultJson) }
                        } else {
                            mainHandler.post { deliverBiometricResult(callbackJsName, """{"success":false,"error":"خطا در بازیابی نشست"}""") }
                        }
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        val msg = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "لغو شد"
                            else -> "خطا در احراز هویت: $errString"
                        }
                        mainHandler.post { deliverBiometricResult(callbackJsName, """{"success":false,"error":"$msg"}""") }
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("ورود با اثر انگشت")
                .setSubtitle("برای ورود به پاژونیک احراز هویت کنید")
                .setNegativeButtonText("لغو")
                .build()
            prompt.authenticate(info)
        }
    }

    private fun deliverBiometricResult(callbackJsName: String, resultJson: String) {
        val arg = escapeJsonForJs(resultJson)
        val js = "typeof window['$callbackJsName']==='function'&&window['$callbackJsName']($arg);"
        webView.evaluateJavascript(js, null)
    }

    /** Escape a string for use inside single-quoted JS string. */
    private fun escapeJsonForJs(s: String): String =
        "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "\\r").replace("\n", "\\n") + "'"

    private fun jsonToUser(json: String): UserEntity? = try {
        val o = JSONObject(json)
        UserEntity(
            id = o.optLong("id", 0L),
            fullName = o.optString("fullName").takeIf { it.isNotEmpty() },
            firstName = o.optString("firstName").takeIf { it.isNotEmpty() },
            lastName = o.optString("lastName").takeIf { it.isNotEmpty() },
            userName = o.optString("userName"),
            phoneNumber = o.optString("phoneNumber"),
            nationalCode = o.optString("nationalCode").takeIf { it.isNotEmpty() },
            avatarUrl = o.optString("avatarUrl").takeIf { it.isNotEmpty() },
            ipAddress = o.optString("ipAddress").takeIf { it.isNotEmpty() },
            password = o.optString("password", ""),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
        )
    } catch (e: Exception) {
        Log.e(TAG, "jsonToUser error", e)
        null
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

    /** For biometric credential storage only; includes password so we can restore user. */
    private fun userToJsonWithPassword(user: UserEntity): String {
        val o = JSONObject(userToJson(user))
        o.put("password", user.password)
        return o.toString()
    }

    companion object {
        private const val TAG = "WebViewBridge"
    }
}
