package com.dicoding.gunungkerinci.data.repository

import com.google.gson.JsonParser

/** Cooldown SOS (HTTP 429): membawa sisa detik untuk ditampilkan ke pengguna. */
internal class SosCooldownException(val waitSeconds: Int) :
    Exception("Tunggu $waitSeconds detik sebelum mengirim SOS lagi")

/** Membaca isi error body dari backend. */
internal object SosApiErrorParser {

    /** Baca `errors.wait_seconds` dari body error 429. Null kalau tidak ada atau bukan JSON. */
    fun waitSeconds(errorBody: String?): Int? {
        if (errorBody.isNullOrBlank()) return null
        return runCatching {
            JsonParser.parseString(errorBody)
                .asJsonObject
                .getAsJsonObject("errors")
                ?.get("wait_seconds")
                ?.asInt
        }.getOrNull()
    }
}
