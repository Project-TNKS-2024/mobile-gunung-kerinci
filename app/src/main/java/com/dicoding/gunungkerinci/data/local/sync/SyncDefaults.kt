package com.dicoding.gunungkerinci.data.local.sync

/**
 * Batas percobaan antrean offline, dipakai worker check-in.
 * Antrean SOS dan chat darurat sengaja tidak dibatasi.
 */
internal object SyncDefaults {

    /** Jumlah percobaan sebelum satu item antrean dianggap menyerah. */
    const val MAX_RETRY = 5
}
