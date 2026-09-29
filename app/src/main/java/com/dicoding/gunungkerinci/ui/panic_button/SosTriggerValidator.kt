package com.dicoding.gunungkerinci.ui.panic_button

/**
 * Validasi pemicu SOS (TDD C01–C04) — murni, tanpa Android.
 *
 * @param wajibPesan true untuk jalur "kirim pesan darurat" (pesan wajib diisi);
 *   panic murni boleh tanpa pesan (backend `message` opsional).
 */
internal object SosTriggerValidator {

    private const val MAX_MESSAGE = 1000
    private val SEVERITIES = setOf("low", "medium", "high")

    fun error(
        latitude: Double?,
        longitude: Double?,
        severity: String?,
        message: String?,
        wajibPesan: Boolean = false
    ): String? {
        if (latitude == null || longitude == null ||
            latitude < -90.0 || latitude > 90.0 ||
            longitude < -180.0 || longitude > 180.0
        ) {
            return "Lokasi GPS tidak valid"
        }

        if (severity.isNullOrBlank() || severity.lowercase() !in SEVERITIES) {
            return "Tingkat SOS tidak valid"
        }

        val pesan = message?.trim().orEmpty()
        if (wajibPesan && pesan.isEmpty()) return "Pesan darurat tidak valid"
        if (pesan.length > MAX_MESSAGE) return "Pesan darurat tidak valid"

        return null
    }
}
