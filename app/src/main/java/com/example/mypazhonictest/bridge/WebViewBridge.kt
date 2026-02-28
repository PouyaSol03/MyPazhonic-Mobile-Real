package com.example.mypazhonictest.bridge

import android.os.Handler
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.mypazhonictest.data.local.entity.LocationEntity
import com.example.mypazhonictest.data.local.entity.LocationType
import com.example.mypazhonictest.data.local.entity.PanelEntity
import com.example.mypazhonictest.data.local.entity.PanelFolderEntity
import com.example.mypazhonictest.data.local.entity.UserEntity
import com.example.mypazhonictest.data.local.prefs.BiometricCredentialStore
import com.example.mypazhonictest.data.local.prefs.BiometricPrefs
import com.example.mypazhonictest.data.repository.LocationRepository
import com.example.mypazhonictest.data.repository.PanelFolderRepository
import com.example.mypazhonictest.data.repository.PanelRepository
import com.example.mypazhonictest.data.repository.UserRepository
import com.example.mypazhonictest.panel.PanelSerialService
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
    private val locationRepository: LocationRepository,
    private val panelRepository: PanelRepository,
    private val panelFolderRepository: PanelFolderRepository,
    private val panelSerialService: PanelSerialService,
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
                BridgeJson.success()
            } catch (e: Exception) {
                Log.e(TAG, "registerUser error", e)
                BridgeJson.error(userFacingMessage(e))
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
                    BridgeJson.loginSuccess(result.token, userToJson(result.user))
                } else {
                    BridgeJson.error("شماره موبایل یا رمز عبور اشتباه است")
                }
            } catch (e: Exception) {
                Log.e(TAG, "login error", e)
                BridgeJson.error(userFacingMessage(e))
            }
        }
    }

    /** Get current session token or null. Returns JSON: { "token": "..." } or { "token": null }. */
    @JavascriptInterface
    fun getSessionToken(): String {
        return runBlocking(Dispatchers.IO) {
            try {
                BridgeJson.tokenResult(userRepository.getSessionToken())
            } catch (e: Exception) {
                Log.e(TAG, "getSessionToken error", e)
                BridgeJson.tokenError(userFacingMessage(e))
            }
        }
    }

    /** Get current user JSON or {"user":null}. Returns user only if session is valid (logged in). */
    @JavascriptInterface
    fun getLatestUser(): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val user = userRepository.getLatestUser()
                if (user != null) BridgeJson.userResult(userToJson(user)) else BridgeJson.userResult("null")
            } catch (e: Exception) {
                Log.e(TAG, "getLatestUser error", e)
                BridgeJson.userNullError(userFacingMessage(e))
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

    /** Get locations by type. type = "COUNTRY" | "STATE" | "COUNTY" | "CITY". Returns JSON: { "locations": [ { id, name, type, parentId, code?, sortOrder }, ... ] } or { "locations": [], "error": "..." }. */
    @JavascriptInterface
    fun getLocationsByType(type: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val locationType = parseLocationType(type)
                    ?: return@runBlocking BridgeJson.locationsError("نوع نامعتبر: $type")
                val list = locationRepository.getByType(locationType)
                BridgeJson.locationsResult(locationsToJsonArray(list))
            } catch (e: Exception) {
                Log.e(TAG, "getLocationsByType error", e)
                BridgeJson.locationsError(userFacingMessage(e))
            }
        }
    }

    /** Get direct children of a parent. parentId = long string (e.g. "1" for country, "10" for Tehran state). Returns same shape as getLocationsByType. */
    @JavascriptInterface
    fun getLocationsByParentId(parentId: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val id = parentId.trim().toLongOrNull()
                    ?: return@runBlocking BridgeJson.locationsError("شناسه والد نامعتبر است")
                val list = locationRepository.getByParentId(id)
                BridgeJson.locationsResult(locationsToJsonArray(list))
            } catch (e: Exception) {
                Log.e(TAG, "getLocationsByParentId error", e)
                BridgeJson.locationsError(userFacingMessage(e))
            }
        }
    }

    /** Get all cities under a state (State → County → City). stateId = long string. */
    @JavascriptInterface
    fun getCitiesByStateId(stateId: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val id = stateId.trim().toLongOrNull()
                    ?: return@runBlocking BridgeJson.locationsError("شناسه استان نامعتبر است")
                val list = locationRepository.getCitiesByStateId(id)
                BridgeJson.locationsResult(locationsToJsonArray(list))
            } catch (e: Exception) {
                Log.e(TAG, "getCitiesByStateId error", e)
                BridgeJson.locationsError(userFacingMessage(e))
            }
        }
    }

    /** Get locations of given type with specific parent. type = "COUNTRY"|"STATE"|"COUNTY"|"CITY", parentId = long string. Returns same shape. */
    @JavascriptInterface
    fun getLocationsByTypeAndParent(type: String, parentId: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val locationType = parseLocationType(type)
                    ?: return@runBlocking BridgeJson.locationsError("نوع نامعتبر: $type")
                val id = parentId.trim().toLongOrNull()
                    ?: return@runBlocking BridgeJson.locationsError("شناسه والد نامعتبر است")
                val list = locationRepository.getByTypeAndParentId(locationType, id)
                BridgeJson.locationsResult(locationsToJsonArray(list))
            } catch (e: Exception) {
                Log.e(TAG, "getLocationsByTypeAndParent error", e)
                BridgeJson.locationsError(userFacingMessage(e))
            }
        }
    }

    private fun parseLocationType(value: String): LocationType? =
        LocationType.entries.find { it.name.equals(value.trim(), ignoreCase = true) }

    private fun locationToJson(loc: LocationEntity): String {
        val o = JSONObject()
        o.put("id", loc.id)
        o.put("name", loc.name)
        o.put("type", loc.type.name)
        o.put("parentId", loc.parentId ?: JSONObject.NULL)
        if (loc.code != null) o.put("code", loc.code)
        o.put("sortOrder", loc.sortOrder)
        return o.toString()
    }

    private fun locationsToJsonArray(list: List<LocationEntity>): String =
        if (list.isEmpty()) "[]" else list.joinToString(prefix = "[", postfix = "]", transform = ::locationToJson)

    private fun currentUserIdOrNull(): Long? = runBlocking(Dispatchers.IO) {
        userRepository.getLatestUser()?.id
    }

    @JavascriptInterface
    fun getPanelsForUser(): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.panelsError("لطفا وارد شوید")
                val list = panelRepository.getByUserId(userId)
                BridgeJson.panelsResult(panelsToJsonArray(list))
            } catch (e: Exception) {
                Log.e(TAG, "getPanelsForUser error", e)
                BridgeJson.panelsError(userFacingMessage(e))
            }
        }
    }

    @JavascriptInterface
    fun getPanelsByFolder(folderIdJson: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.panelsError("لطفا وارد شوید")
                val folderId = folderIdJson.trim().let { if (it.isEmpty() || it == "null") null else it.toLongOrNull() }
                val list = panelRepository.getByUserIdAndFolderId(userId, folderId)
                BridgeJson.panelsResult(panelsToJsonArray(list))
            } catch (e: Exception) {
                Log.e(TAG, "getPanelsByFolder error", e)
                BridgeJson.panelsError(userFacingMessage(e))
            }
        }
    }

    @JavascriptInterface
    fun createPanel(panelJson: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.error("لطفا وارد شوید")
                val entity = jsonToPanel(panelJson) ?: return@runBlocking BridgeJson.error("داده پنل نامعتبر است")
                val withUser = entity.copy(userId = userId, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
                val id = panelRepository.insert(withUser)
                org.json.JSONObject().apply { put("success", true); put("id", id) }.toString()
            } catch (e: Exception) {
                Log.e(TAG, "createPanel error", e)
                BridgeJson.error(userFacingMessage(e))
            }
        }
    }

    @JavascriptInterface
    fun updatePanel(panelJson: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.error("لطفا وارد شوید")
                val entity = jsonToPanel(panelJson) ?: return@runBlocking BridgeJson.error("داده پنل نامعتبر است")
                val existing = panelRepository.getById(entity.id) ?: return@runBlocking BridgeJson.error("پنل یافت نشد")
                if (existing.userId != userId) return@runBlocking BridgeJson.error("دسترسی غیرمجاز")
                val merged = existing.copy(
                    name = entity.name,
                    folderId = entity.folderId,
                    icon = entity.icon,
                    gsmPhone = entity.gsmPhone,
                    ip = entity.ip,
                    port = entity.port,
                    code = entity.code,
                    description = entity.description,
                    serialNumber = entity.serialNumber,
                    isActive = entity.isActive,
                    locationId = entity.locationId,
                    codeUD = entity.codeUD,
                    lastStatus = entity.lastStatus,
                    updatedAt = System.currentTimeMillis()
                )
                panelRepository.update(merged)
                BridgeJson.success()
            } catch (e: Exception) {
                Log.e(TAG, "updatePanel error", e)
                BridgeJson.error(userFacingMessage(e))
            }
        }
    }

    @JavascriptInterface
    fun deletePanel(panelId: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.error("لطفا وارد شوید")
                val id = panelId.trim().toLongOrNull() ?: return@runBlocking BridgeJson.error("شناسه پنل نامعتبر است")
                val existing = panelRepository.getById(id) ?: return@runBlocking BridgeJson.error("پنل یافت نشد")
                if (existing.userId != userId) return@runBlocking BridgeJson.error("دسترسی غیرمجاز")
                panelRepository.deleteById(id)
                BridgeJson.success()
            } catch (e: Exception) {
                Log.e(TAG, "deletePanel error", e)
                BridgeJson.error(userFacingMessage(e))
            }
        }
    }

    @JavascriptInterface
    fun setPanelFolder(panelId: String, folderId: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.error("لطفا وارد شوید")
                val pId = panelId.trim().toLongOrNull() ?: return@runBlocking BridgeJson.error("شناسه پنل نامعتبر است")
                val fId = folderId.trim().let { if (it.isEmpty() || it == "null") null else it.toLongOrNull() }
                val existing = panelRepository.getById(pId) ?: return@runBlocking BridgeJson.error("پنل یافت نشد")
                if (existing.userId != userId) return@runBlocking BridgeJson.error("دسترسی غیرمجاز")
                panelRepository.setPanelFolder(pId, fId)
                BridgeJson.success()
            } catch (e: Exception) {
                Log.e(TAG, "setPanelFolder error", e)
                BridgeJson.error(userFacingMessage(e))
            }
        }
    }

    @JavascriptInterface
    fun setPanelLastStatus(panelId: String, lastStatus: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.error("لطفا وارد شوید")
                val pId = panelId.trim().toLongOrNull() ?: return@runBlocking BridgeJson.error("شناسه پنل نامعتبر است")
                val existing = panelRepository.getById(pId) ?: return@runBlocking BridgeJson.error("پنل یافت نشد")
                if (existing.userId != userId) return@runBlocking BridgeJson.error("دسترسی غیرمجاز")
                val status = lastStatus.trim().takeIf { it.isNotEmpty() }
                panelRepository.setPanelLastStatus(pId, status)
                BridgeJson.success()
            } catch (e: Exception) {
                Log.e(TAG, "setPanelLastStatus error", e)
                BridgeJson.error(userFacingMessage(e))
            }
        }
    }

    @JavascriptInterface
    fun getFolders(): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.foldersError("لطفا وارد شوید")
                val list = panelFolderRepository.getByUserId(userId)
                BridgeJson.foldersResult(foldersToJsonArray(list))
            } catch (e: Exception) {
                Log.e(TAG, "getFolders error", e)
                BridgeJson.foldersError(userFacingMessage(e))
            }
        }
    }

    @JavascriptInterface
    fun createFolder(name: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.error("لطفا وارد شوید")
                val trimmed = name.trim().takeIf { it.isNotEmpty() } ?: return@runBlocking BridgeJson.error("نام پوشه الزامی است")
                val folder = PanelFolderEntity(userId = userId, name = trimmed)
                val id = panelFolderRepository.insert(folder)
                org.json.JSONObject().apply { put("success", true); put("id", id) }.toString()
            } catch (e: Exception) {
                Log.e(TAG, "createFolder error", e)
                BridgeJson.error(userFacingMessage(e))
            }
        }
    }

    @JavascriptInterface
    fun updateFolder(folderId: String, name: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.error("لطفا وارد شوید")
                val id = folderId.trim().toLongOrNull() ?: return@runBlocking BridgeJson.error("شناسه پوشه نامعتبر است")
                val trimmed = name.trim().takeIf { it.isNotEmpty() } ?: return@runBlocking BridgeJson.error("نام پوشه الزامی است")
                val existing = panelFolderRepository.getById(id) ?: return@runBlocking BridgeJson.error("پوشه یافت نشد")
                if (existing.userId != userId) return@runBlocking BridgeJson.error("دسترسی غیرمجاز")
                panelFolderRepository.update(existing.copy(name = trimmed))
                BridgeJson.success()
            } catch (e: Exception) {
                Log.e(TAG, "updateFolder error", e)
                BridgeJson.error(userFacingMessage(e))
            }
        }
    }

    @JavascriptInterface
    fun deleteFolder(folderId: String): String {
        return runBlocking(Dispatchers.IO) {
            try {
                val userId = currentUserIdOrNull()
                    ?: return@runBlocking BridgeJson.error("لطفا وارد شوید")
                val id = folderId.trim().toLongOrNull() ?: return@runBlocking BridgeJson.error("شناسه پوشه نامعتبر است")
                val existing = panelFolderRepository.getById(id) ?: return@runBlocking BridgeJson.error("پوشه یافت نشد")
                if (existing.userId != userId) return@runBlocking BridgeJson.error("دسترسی غیرمجاز")
                panelRepository.clearFolderIdForFolder(id)
                panelFolderRepository.deleteById(id)
                BridgeJson.success()
            } catch (e: Exception) {
                Log.e(TAG, "deleteFolder error", e)
                BridgeJson.error(userFacingMessage(e))
            }
        }
    }

    /** Get serial number from panel via TCP. Params: codeUD, ip, port (string). Returns { serialNumber } or { error }. */
    @JavascriptInterface
    fun getSerialNumber(codeUD: String, ip: String, port: String): String {
        Log.d(TAG, "getSerialNumber request: codeUD=${codeUD.take(8)}..., ip=$ip, port=$port")
        return runBlocking(Dispatchers.IO) {
            try {
                if (codeUD.isBlank()) {
                    Log.w(TAG, "getSerialNumber: codeUD is blank")
                    return@runBlocking BridgeJson.serialNumberError("کد آپلود دانلود اجباری است")
                }
                if (ip.isBlank() || port.isBlank()) {
                    Log.w(TAG, "getSerialNumber: ip or port is blank")
                    return@runBlocking BridgeJson.serialNumberError("آی پی و پورت اجباری هستند")
                }
                val portInt = port.trim().toIntOrNull()
                if (portInt == null) {
                    Log.w(TAG, "getSerialNumber: invalid port=$port")
                    return@runBlocking BridgeJson.serialNumberError("پورت نامعتبر است")
                }
                val result = panelSerialService.getSerialNumber(codeUD.trim(), ip.trim(), portInt)
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "getSerialNumber success: serialNumber=${it.take(12)}...")
                        BridgeJson.serialNumberResult(it)
                    },
                    onFailure = {
                        Log.e(TAG, "getSerialNumber failure", it)
                        BridgeJson.serialNumberError(userFacingMessage(it))
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "getSerialNumber error", e)
                BridgeJson.serialNumberError(userFacingMessage(e))
            }
        }
    }

    private fun panelToJson(p: PanelEntity): String {
        val o = JSONObject()
        o.put("id", p.id)
        o.put("userId", p.userId)
        o.put("folderId", p.folderId ?: JSONObject.NULL)
        o.put("icon", p.icon ?: JSONObject.NULL)
        o.put("name", p.name)
        o.put("gsmPhone", p.gsmPhone ?: JSONObject.NULL)
        o.put("ip", p.ip ?: JSONObject.NULL)
        o.put("port", p.port ?: JSONObject.NULL)
        o.put("code", p.code ?: JSONObject.NULL)
        o.put("description", p.description ?: JSONObject.NULL)
        o.put("serialNumber", p.serialNumber ?: JSONObject.NULL)
        o.put("isActive", p.isActive)
        o.put("locationId", p.locationId ?: JSONObject.NULL)
        o.put("codeUD", p.codeUD ?: JSONObject.NULL)
        o.put("lastStatus", p.lastStatus ?: JSONObject.NULL)
        o.put("createdAt", p.createdAt)
        o.put("updatedAt", p.updatedAt)
        return o.toString()
    }

    private fun panelsToJsonArray(list: List<PanelEntity>): String =
        if (list.isEmpty()) "[]" else list.joinToString(prefix = "[", postfix = "]", transform = ::panelToJson)

    private fun jsonToPanel(json: String): PanelEntity? {
        return try {
            val o = JSONObject(json)
            val name = o.optString("name").takeIf { it.isNotEmpty() } ?: return null
            PanelEntity(
                id = o.optLong("id", 0L),
                userId = 0L,
                folderId = o.optString("folderId").takeIf { it.isNotEmpty() }?.toLongOrNull(),
                icon = o.optString("icon").takeIf { it.isNotEmpty() },
                name = name,
                gsmPhone = o.optString("gsmPhone").takeIf { it.isNotEmpty() },
                ip = o.optString("ip").takeIf { it.isNotEmpty() },
                port = o.optInt("port", -1).takeIf { it >= 0 },
                code = o.optString("code").takeIf { it.isNotEmpty() },
                description = o.optString("description").takeIf { it.isNotEmpty() },
                serialNumber = o.optString("serialNumber").takeIf { it.isNotEmpty() },
                isActive = o.optBoolean("isActive", true),
                locationId = o.optString("locationId").takeIf { it.isNotEmpty() }?.toLongOrNull() ?: o.optLong("locationId", -1L).let { if (it >= 0) it else null },
                codeUD = o.optString("codeUD").takeIf { it.isNotEmpty() },
                lastStatus = o.optString("lastStatus").takeIf { it.isNotEmpty() },
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            Log.e(TAG, "jsonToPanel error", e)
            null
        }
    }

    private fun folderToJson(f: PanelFolderEntity): String {
        val o = JSONObject()
        o.put("id", f.id)
        o.put("userId", f.userId)
        o.put("name", f.name)
        o.put("sortOrder", f.sortOrder)
        return o.toString()
    }

    private fun foldersToJsonArray(list: List<PanelFolderEntity>): String =
        if (list.isEmpty()) "[]" else list.joinToString(prefix = "[", postfix = "]", transform = ::folderToJson)

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
                deliverBiometricResult(callbackJsName, BridgeJson.error("ورود با بیومتریک غیرفعال است"))
                return@post
            }
            if (!biometricCredentialStore.hasStoredCredentials()) {
                deliverBiometricResult(callbackJsName, BridgeJson.error("ابتدا با رمز عبور وارد شوید"))
                return@post
            }
            val biometricManager = BiometricManager.from(activity)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> { /* proceed */ }
                else -> {
                    deliverBiometricResult(callbackJsName, BridgeJson.error("احراز هویت بیومتریک در دسترس نیست"))
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
                            mainHandler.post { deliverBiometricResult(callbackJsName, BridgeJson.loginSuccess(token, userJson)) }
                        } else {
                            mainHandler.post { deliverBiometricResult(callbackJsName, BridgeJson.error("خطا در بازیابی نشست")) }
                        }
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        val msg = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> "لغو شد"
                            else -> "خطا در احراز هویت: $errString"
                        }
                        mainHandler.post { deliverBiometricResult(callbackJsName, BridgeJson.error(msg)) }
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

    /** Single place for user-facing error messages from exceptions (Persian). */
    private fun userFacingMessage(e: Throwable): String =
        e.message?.takeIf { it.isNotEmpty() }?.replace("\"", "'")
            ?: "خطایی رخ داد. لطفا دوباره تلاش کنید."

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
