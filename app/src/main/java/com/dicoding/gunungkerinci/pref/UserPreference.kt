package com.dicoding.gunungkerinci.pref

import android.content.Context

class UserPreference(context: Context) {
    private val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        pref.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return pref.getString(KEY_TOKEN, null)
    }

    fun clearToken() {
        pref.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "user_pref"
        private const val KEY_TOKEN = "token"
    }
}