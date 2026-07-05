package com.course.imchat.data.cache

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistent session storage — like HTTP cookies.
 * Stores server URL, session token, and last credentials.
 * Survives app restarts for instant re-login.
 */
class SessionCache(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("imchat_session", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var sessionToken: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var nickname: String
        get() = prefs.getString(KEY_NICKNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NICKNAME, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    fun hasSession(): Boolean = sessionToken.isNotEmpty() && serverUrl.isNotEmpty()

    fun saveSession(url: String, token: String, user: String, nick: String) {
        prefs.edit()
            .putString(KEY_SERVER_URL, url)
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, user)
            .putString(KEY_NICKNAME, nick)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TOKEN = "session_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_DARK_MODE = "dark_mode"
    }
}
