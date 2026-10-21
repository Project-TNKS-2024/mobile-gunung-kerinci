package com.dicoding.gunungkerinci.model

/**
 * Fase perjalanan pendaki. Nilai serialized harus persis "naik"/"turun" karena
 * dikirim ke backend dan disimpan di Room apa adanya.
 */
enum class TrackingPhase(val serialized: String) {
    NAIK("naik"),
    TURUN("turun");

    companion object {
        fun from(value: String?): TrackingPhase =
            entries.firstOrNull { it.serialized == value } ?: NAIK
    }
}
