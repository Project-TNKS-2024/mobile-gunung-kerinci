package com.dicoding.gunungkerinci.util

import android.content.Context

// Penanda pemilik cache peringatan dini. id_user tidak disimpan lokal, jadi pakai
// email (ditulis saat login), atau token sebagai cadangan.
internal object SessionOwner {
    private const val PREF_AUTH = "auth"
    private const val KEY_EMAIL = "email"
    private const val KEY_TOKEN = "token"

    fun currentKey(context: Context): String {
        val pref = context.getSharedPreferences(PREF_AUTH, Context.MODE_PRIVATE)
        return pref.getString(KEY_EMAIL, "").orEmpty()
            .ifBlank { pref.getString(KEY_TOKEN, "").orEmpty() }
    }
}
