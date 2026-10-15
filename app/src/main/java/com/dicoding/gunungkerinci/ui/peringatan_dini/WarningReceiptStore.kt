package com.dicoding.gunungkerinci.ui.peringatan_dini

import android.content.Context
import java.security.MessageDigest

/** Tanda terima terpisah dari cache; daftar kosong tidak mereset peringatan yang sudah diterima. */
internal class WarningReceiptStore(context: Context) {
    private val prefs = context.getSharedPreferences("warning_receipts", Context.MODE_PRIVATE)

    fun unseen(token: String, warnings: List<PeringatanDini>): List<PeringatanDini> {
        val seen = prefs.getStringSet(sessionKey(token), emptySet()).orEmpty()
        return warnings.filter { receiptKey(it) !in seen }
    }

    fun markReceived(token: String, warnings: List<PeringatanDini>) {
        val key = sessionKey(token)
        val seen = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        seen.addAll(warnings.map(::receiptKey))
        prefs.edit().putStringSet(key, seen).apply()
    }

    fun clear() = prefs.edit().clear().apply()
    private fun receiptKey(warning: PeringatanDini) = "${warning.id}:${warning.severity.lowercase()}"

    companion object {
        fun sessionKey(token: String): String = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
