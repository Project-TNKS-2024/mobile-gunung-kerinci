package com.dicoding.gunungkerinci.util

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar
import java.text.DateFormatSymbols

internal object DateTimeFormat {

    private const val ZONA_WIB = "Asia/Jakarta"

    // Desimal mengikuti desain UI: "14.33", bukan "14:33"
    private const val POLA_JAM_MENIT = "HH.mm"

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

    private val apiPattern = Regex("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})(?:\\.\\d{1,9})?\\+07:00$")

    /** API owns the WIB clock. Validate and display its fields without a timezone conversion. */
    fun apiWibTimestamp(iso: String?): String? {
        val value = iso?.let { apiPattern.matchEntire(it) }?.groupValues?.get(1) ?: return null
        return runCatching {
            Calendar.getInstance(TimeZone.getTimeZone(ZONA_WIB)).apply {
                clear()
                isLenient = false
                set(value.substring(0, 4).toInt(), value.substring(5, 7).toInt() - 1,
                    value.substring(8, 10).toInt(), value.substring(11, 13).toInt(),
                    value.substring(14, 16).toInt(), value.substring(17, 19).toInt())
                timeInMillis // Validate date fields only; this value is not used for display.
            }
            value
        }.getOrNull()
    }

    fun apiWibHm(iso: String?): String? = apiWibTimestamp(iso)?.substring(11, 16)?.replace(':', '.')

    fun apiWibDate(iso: String?): String? = apiWibTimestamp(iso)?.take(10)

    fun calendarDate(date: String?): String? = date?.takeIf {
        Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(it) && apiWibTimestamp("${it}T00:00:00+07:00") != null
    }

    fun apiWibDateTime(iso: String?): String? = apiWibTimestamp(iso)?.let {
        "${it.take(10)} ${it.substring(11, 16)} WIB"
    }

    fun apiWibLongDateTime(iso: String?): String? = apiWibTimestamp(iso)?.let {
        val month = DateFormatSymbols(Locale("id", "ID")).shortMonths[it.substring(5, 7).toInt() - 1]
        "${it.substring(8, 10)} $month ${it.take(4)}, ${it.substring(11, 16)} WIB"
    }

    /** Epoch parsing is for ordering/persistence, never for formatting API timestamps. */
    fun isoToEpochMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        val normalized = iso.trim().replace(Regex("\\.\\d+(?=Z|[+-]\\d{2}:?\\d{2}$)"), "")
        val position = ParsePosition(0)
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            isLenient = false
        }.parse(normalized, position)?.takeIf { position.index == normalized.length }?.time
    }
}
