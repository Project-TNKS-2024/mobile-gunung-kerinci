package com.dicoding.gunungkerinci.ui.panic_button

import java.io.IOException

/** Pesan sistem seperti "unable to resolve host" diganti kalimat yang jelas. */
internal fun pesanJaringan(error: Throwable, fallback: String): String =
    if (error is IOException) "Tidak ada koneksi internet. Coba lagi saat jaringan tersedia."
    else error.message ?: fallback
