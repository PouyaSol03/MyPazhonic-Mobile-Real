package com.example.mypazhonictest.bridge

import org.json.JSONObject

/**
 * Central place for all bridge JSON responses (Contract with React).
 * Keeps WebViewBridge free of raw JSON string building and ensures consistent shape.
 */
object BridgeJson {

    fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")

    fun success(): String = """{"success":true}"""

    fun error(message: String): String =
        """{"success":false,"error":"${escape(message)}"}"""

    fun tokenResult(token: String?): String =
        if (token != null) """{"token":"${escape(token)}"}""" else """{"token":null}"""

    fun tokenError(message: String): String =
        """{"token":null,"error":"${escape(message)}"}"""

    fun userResult(userJson: String): String =
        """{"user":$userJson}"""

    fun userNullError(message: String): String =
        """{"user":null,"error":"${escape(message)}"}"""

    fun loginSuccess(token: String, userJson: String): String =
        """{"success":true,"token":"${escape(token)}","user":$userJson}"""

    /** Locations list response: { "locations": [ ... ] } */
    fun locationsResult(locationsJsonArray: String): String =
        """{"locations":$locationsJsonArray}"""

    fun locationsError(message: String): String =
        """{"locations":[],"error":"${escape(message)}"}"""

    /** Panels list: { "panels": [ ... ] } */
    fun panelsResult(panelsJsonArray: String): String =
        """{"panels":$panelsJsonArray}"""

    fun panelsError(message: String): String =
        """{"panels":[],"error":"${escape(message)}"}"""

    /** Folders list: { "folders": [ ... ] } */
    fun foldersResult(foldersJsonArray: String): String =
        """{"folders":$foldersJsonArray}"""

    fun foldersError(message: String): String =
        """{"folders":[],"error":"${escape(message)}"}"""
}
