package com.dicoding.gunungkerinci.ui.panic_button

import java.io.IOException

/**
 * Aturan antrean SOS offline (TDD C06/C07) — murni, tanpa Android.
 * Dipakai saat implementasi Room + WorkManager (Phase 4b).
 */
internal object SosQueueRule {

    /** C06: error jaringan → simpan ke antrean lokal (nanti dikirim ulang). */
    fun simpanOffline(error: Throwable): Boolean = error is IOException

    /** C07: keputusan kirim ulang saat koneksi tersedia. */
    fun harusKirimUlang(code: Int?, error: Throwable?): Boolean {
        if (error != null) return error is IOException
        if (code == null) return false
        return code == 408 || code == 429 || code >= 500
    }
}
