package com.dicoding.gunungkerinci.data.local.sync

/**
 * Batas percobaan antrean offline, dipakai worker check-in.
 * Antrean SOS dan chat darurat tidak dibatasi.
 */
internal object SyncDefaults {

    /** Jumlah percobaan sebelum satu item antrean menyerah. */
    const val MAX_RETRY = 5

    /** Kunci input data WorkManager untuk menyaring antrean per booking. */
    const val BOOKING_ID = "bookingId"
}
