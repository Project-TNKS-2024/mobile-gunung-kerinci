package com.dicoding.gunungkerinci.ui.peringatan_dini

internal object PeringatanDiniSelector {
    // Ambil satu peringatan paling berat; kalau sama, ambil yang paling baru.
    fun select(warnings: List<PeringatanDini>): PeringatanDini? =
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
