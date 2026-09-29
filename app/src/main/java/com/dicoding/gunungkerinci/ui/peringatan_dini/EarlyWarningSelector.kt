package com.dicoding.gunungkerinci.ui.peringatan_dini

/**
 * Memilih SATU peringatan yang ditampilkan sebagai peringatan aktif:
 * severity tertinggi; bila seri, `created_at` paling baru.
 *
 * Catatan: server mengembalikan peringatan aktif urut `created_at desc`
 * tanpa pengurutan severity, jadi pemilihan dilakukan di client.
 */
internal object EarlyWarningSelector {

    fun select(warnings: List<EmergencyWarning>): EmergencyWarning? =
        warnings.maxWithOrNull(
            compareBy({ severityRank(it.severity) }, { it.createdAt })
        )

    private fun severityRank(severity: String): Int = when (severity.lowercase()) {
        "critical" -> 4
        "high" -> 3
        "medium" -> 2
        "low" -> 1
        else -> 0
    }
}
