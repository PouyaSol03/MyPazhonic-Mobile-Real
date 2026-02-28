package com.example.mypazhonictest.panel

import android.util.Log
import com.example.mypazhonictest.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Service for panel serial-number and similar operations.
 * Sends to panel at ip:port exactly: {userPC}-{pId}-0-{codeUD}-GetSerialNumber;@
 * where userPC = userId (or phoneNumber) of logged-in user, pId = device processor id.
 */
class PanelSerialService @Inject constructor(
    private val tcpClient: PanelTcpClient,
    private val userRepository: UserRepository
) {

    private companion object {
        const val TAG = "PanelSerialService"
    }

    /**
     * Get serial number from panel at given ip:port.
     * Payload: userPC (userId or phoneNumber) - pId (device) - 0 - codeUD - GetSerialNumber;@
     */
    suspend fun getSerialNumber(codeUD: String, ip: String, port: Int): Result<String> =
        withContext(Dispatchers.IO) {
            val code = codeUD.trim()
            val host = ip.trim()
            if (code.isEmpty()) return@withContext Result.failure(Exception("کد آپلود دانلود اجباری است"))
            if (host.isEmpty() || port <= 0) return@withContext Result.failure(Exception("آی‌پی و پورت اجباری هستند"))
            val user = userRepository.getLatestUser()
                ?: return@withContext Result.failure(Exception("لطفا وارد شوید"))
            val userPC = user.id.toString().takeIf { it.isNotBlank() }
                ?: user.phoneNumber.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(Exception("شناسه یا شماره کاربر یافت نشد"))
            Log.d(TAG, "getSerialNumber userPC (userId/phone)=$userPC")
            try {
                val pId = PanelProtocol.getDeviceProcessorId()
                Log.d(TAG, "getSerialNumber processId (pId)=$pId")
                val request = PanelProtocol.buildGetSerialNumberRequest(userPC, code)
                Log.d(TAG, "getSerialNumber connect $host:$port")
                Log.d(TAG, "getSerialNumber send to panel (full data): $request")
                tcpClient.connect(host, port)
                val response = tcpClient.sendAndReceive(request)
                Log.d(TAG, "getSerialNumber response length=${response.length}")
                PanelProtocol.parseGetSerialNumberResponse(response)
            } catch (e: Exception) {
                Log.e(TAG, "getSerialNumber TCP/parse error", e)
                Result.failure(e)
            } finally {
                tcpClient.disconnect()
            }
        }
}
