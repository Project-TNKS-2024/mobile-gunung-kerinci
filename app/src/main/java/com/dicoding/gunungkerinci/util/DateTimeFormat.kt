package com.dicoding.gunungkerinci.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object DateTimeFormat {

    private const val ZONA_WIB = "Asia/Jakarta"
    private const val ZONA_UTC = "UTC"

    // Desimal mengikuti desain UI: "14.33", bukan "14:33"
    private const val POLA_JAM_MENIT = "HH.mm"
    private const val POLA_ISO = "yyyy-MM-dd'T'HH:mm:ss"

    // Waktu sekarang (WIB), format "HH.mm"
    fun nowWibHm(): String =
        SimpleDateFormat(POLA_JAM_MENIT, Locale.US)
            .apply { timeZone = TimeZone.getTimeZone(ZONA_WIB) }
            .format(Date())

    // Epoch millis jadi "HH.mm" WIB. null tetap null.
    fun epochToWibHm(epochMillis: Long?): String? =
        epochMillis?.let {
            SimpleDateFormat(POLA_JAM_MENIT, Locale.US)
                .apply { timeZone = TimeZone.getTimeZone(ZONA_WIB) }
                .format(Date(it))
        }

    // ISO-8601 UTC dari backend jadi "HH.mm" WIB. Gagal parse jadi null.
    fun isoToWibHm(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        val bersih = iso.trim().substringBefore('.').removeSuffix("Z")
        val waktu = runCatching {
            SimpleDateFormat(POLA_ISO, Locale.US).apply {
                timeZone = TimeZone.getTimeZone(ZONA_UTC)
                isLenient = false
            }.parse(bersih)
        }.getOrNull() ?: return null
        return SimpleDateFormat(POLA_JAM_MENIT, Locale.US).apply {
            timeZone = TimeZone.getTimeZone(ZONA_WIB)
        }.format(waktu)
    }
}
