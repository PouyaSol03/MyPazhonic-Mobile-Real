package com.example.mypazhonictest.data.repository

import com.example.mypazhonictest.data.local.dao.UserDao
import com.example.mypazhonictest.data.local.entity.UserEntity
import com.example.mypazhonictest.data.local.prefs.SessionPrefs
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class LoginResult(
    val token: String,
    val user: UserEntity
)

class UserRepository(
    private val userDao: UserDao,
    private val sessionPrefs: SessionPrefs
) {
    fun observeLatestUser(): Flow<UserEntity?> = userDao.observeLatestUser()

    /** Returns the current user only if there is a valid session (token). Uses session userId so the logged-in user (with nationalCode etc.) is returned. */
    suspend fun getLatestUser(): UserEntity? {
        val token = sessionPrefs.getSessionToken() ?: return null
        if (token.isEmpty()) return null
        val userId = sessionPrefs.getUserId() ?: return userDao.getLatestUser()
        return userDao.getById(userId) ?: userDao.getLatestUser()
    }

    suspend fun registerLocalUser(user: UserEntity): Long {
        return userDao.upsert(user)
    }

    /**
     * Login with phone and password. On success generates a session token, stores it, and returns token + user.
     */
    suspend fun loginByPhoneAndPassword(
        phoneNumber: String,
        password: String
    ): LoginResult? {
        val user = userDao.getLatestUser() ?: return null
        if (user.phoneNumber != phoneNumber || user.password != password) return null
        val token = UUID.randomUUID().toString()
        sessionPrefs.setSession(token, user.id)
        return LoginResult(token = token, user = user)
    }

    suspend fun logoutLocal() {
        sessionPrefs.clearSession()
        userDao.deleteAll()
    }

    /**
     * Restore session and user for biometric login (after logout, user is re-inserted and session set).
     */
    suspend fun restoreSessionAndUser(token: String, user: UserEntity) {
        userDao.upsert(user)
        sessionPrefs.setSession(token, user.id)
    }

    /** Returns current session token or null if not logged in. */
    suspend fun getSessionToken(): String? = sessionPrefs.getSessionToken()
}
