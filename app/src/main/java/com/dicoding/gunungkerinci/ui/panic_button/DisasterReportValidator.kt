package com.dicoding.gunungkerinci.ui.panic_button

/**
 * Validasi laporan potensi bencana (TDD C05) — murni, tanpa Android.
 * Aturan lampiran mengikuti backend: jpeg/png/jpg, maksimal 5 MB.
 */
internal object DisasterReportValidator {

    private val ALLOWED_MIME = setOf("image/jpeg", "image/jpg", "image/png")
    private const val MAX_FILE_BYTES = 5L * 1024 * 1024

    fun error(
        potensiBencana: String?,
        deskripsi: String?,
        lokasi: String?,
        lampiranMime: String? = null,
        lampiranBytes: Long? = null
    ): String? {
        if (potensiBencana.isNullOrBlank()) return "Potensi bencana wajib diisi"
        if (deskripsi.isNullOrBlank()) return "Deskripsi wajib diisi"
        if (lokasi.isNullOrBlank()) return "Lokasi wajib diisi"

        if (lampiranMime != null) {
            if (lampiranMime.lowercase() !in ALLOWED_MIME) return "Lampiran tidak valid"
            if (lampiranBytes != null && lampiranBytes > MAX_FILE_BYTES) return "Lampiran tidak valid"
        }

        return null
    }
}
