package com.dicoding.gunungkerinci.data.sync

import java.io.IOException

/**
 * Aturan antrean SOS offline (TDD C06/C07) — murni, tanpa Android.
 * Dipakai bersama oleh ViewModel (saat memutuskan menyimpan offline)
 * dan PendingSosSyncWorker (saat memutuskan mencoba lagi).
 */
internal object SosQueueRule {

    /** C06: error jaringan → simpan ke antrean lokal (nanti dikirim ulang). */
    fun simpanOffline(error: Throwable): Boolean = error is IOException

    /** C07: keputusan kirim ulang saat koneksi tersedia. */
    fun harusKirimUlang(code: Int?, error: Throwable?): Boolean {
        if (error != null) return error is IOException
        if (code == null) return false
        // 401 = token kedaluwarsa, masih bisa berhasil setelah pendaki login ulang.
        return code == 401 || code == 408 || code == 429 || code >= 500
    }
}
