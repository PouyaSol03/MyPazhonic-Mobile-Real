package com.example.mypazhonictest.panel

import android.os.Build

/**
 * Panel protocol: build request payloads and parse responses.
 * Reusable for GetSerialNumber and similar commands.
 */
object PanelProtocol {

    /**
     * Build request string for GetSerialNumber command.
     * Format: {userPC}-{processorId}-{0}-{codeUD}-GetSerialNumber;@
     */
    fun buildGetSerialNumberRequest(
        userPersonalCode: String,
        codeUD: String,
        processorId: String = getDeviceProcessorId()
    ): String = "$userPersonalCode-$processorId-0-$codeUD-GetSerialNumber;@"

    /**
     * Device identifier for protocol (Android equivalent of processor id).
     * Dots are removed so e.g. TP1A.220624.014 becomes TP1A220624014.
     */
    fun getDeviceProcessorId(): String {
        val raw = Build.SERIAL.takeIf { it.isNotBlank() && it != "unknown" } ?: Build.ID.ifBlank { "0" }
        return raw.replace(".", "")
    }

    /**
     * Parse GetSerialNumber response.
     * If response contains "NotValid" returns Error, else slice(0,-2) and split(':')[1].
     */
    fun parseGetSerialNumberResponse(response: String): Result<String> {
        val trimmed = response.trim()
        if (trimmed.contains("NotValid", ignoreCase = true)) {
            return Result.failure(Exception(trimmed))
        }
        val withoutSuffix = trimmed.dropLast(2).trim()
        val parts = withoutSuffix.split(':')
        val value = parts.getOrNull(1)?.trim() ?: ""
        return if (value.isNotEmpty()) Result.success(value)
        else Result.failure(Exception("پاسخ نامعتبر: $trimmed"))
    }
}
