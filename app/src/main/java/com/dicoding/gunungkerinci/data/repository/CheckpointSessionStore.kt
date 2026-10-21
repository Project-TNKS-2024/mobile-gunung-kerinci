package com.dicoding.gunungkerinci.data.repository

import android.content.Context
import com.dicoding.gunungkerinci.model.MyTiketItem
import com.dicoding.gunungkerinci.model.TrackingProgressV2Data
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.security.MessageDigest

/** Snapshot API untuk membuka perjalanan yang sudah dimuat ketika offline.
 * Memakai preferences login yang ikut dibersihkan saat logout; setiap entry terikat token.
 */
internal class CheckpointSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("user_pref", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveBookings(token: String, data: List<MyTiketItem>) = save(token, "bookings", data)
    fun readBookings(token: String): List<MyTiketItem>? = read(
        token, "bookings", object : TypeToken<List<MyTiketItem>>() {}.type
    )

    fun saveIdentity(token: String, data: IdentitasSaya) = save(token, "identity", data)
    fun readIdentity(token: String): IdentitasSaya? = read(token, "identity", IdentitasSaya::class.java)

    fun saveProgress(token: String, bookingId: String, data: TrackingProgressV2Data) =
        save(token, "progress_$bookingId", data)
    fun readProgress(token: String, bookingId: String): TrackingProgressV2Data? =
        read(token, "progress_$bookingId", TrackingProgressV2Data::class.java)

    private fun key(token: String, name: String): String {
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(token.removePrefix("Bearer ").toByteArray()).joinToString("") { "%02x".format(it) }
        return "checkpoint_${fingerprint}_$name"
    }

    private fun save(token: String, name: String, data: Any) {
        preferences.edit().putString(key(token, name), gson.toJson(data)).apply()
    }

    private fun <T> read(token: String, name: String, type: Type): T? = runCatching {
        preferences.getString(key(token, name), null)?.let { gson.fromJson<T>(it, type) }
    }.getOrNull()
}
