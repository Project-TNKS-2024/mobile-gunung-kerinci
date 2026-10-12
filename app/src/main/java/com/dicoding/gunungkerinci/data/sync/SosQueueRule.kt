package com.dicoding.gunungkerinci.data.sync

import java.io.IOException

/**
 * Aturan antrean SOS offline, dipakai ViewModel dan PendingSosSyncWorker.
 */
internal object SosQueueRule {

    /** Error jaringan berarti pesan belum sampai, jadi disimpan ke antrean. */
    fun simpanOffline(error: Throwable): Boolean = error is IOException

    /** Kode yang masih layak dicoba lagi saat jaringan tersedia. */
    fun harusKirimUlang(code: Int?, error: Throwable?): Boolean {
        if (error != null) return error is IOException
        if (code == null) return false
        // 401 = token kedaluwarsa, masih bisa berhasil setelah pendaki login ulang.
        return code == 401 || code == 408 || code == 429 || code >= 500
    }
}
