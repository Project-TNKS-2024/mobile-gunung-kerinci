package com.dicoding.gunungkerinci.ui.panic_button

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream
import java.io.OutputStream

/**
 * Tulis ulang gambar jadi JPEG dengan sisi terpanjang maksimal [SISI_MAKS] piksel.
 * Dipakai sebelum kirim supaya upload tidak berat saat sinyal jelek.
 */
internal fun simpanGambarKecil(input: InputStream, output: OutputStream): Boolean {
    val isi = input.readBytes()

    // Baca ukuran aslinya dulu supaya tidak perlu memuat gambar penuh ke memori.
    val ukuran = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(isi, 0, isi.size, ukuran)
    if (ukuran.outWidth <= 0 || ukuran.outHeight <= 0) return false

    var sampel = 1
    while (maxOf(ukuran.outWidth, ukuran.outHeight) / (sampel * 2) >= SISI_MAKS) sampel *= 2

    val gambar = BitmapFactory.decodeByteArray(
        isi, 0, isi.size, BitmapFactory.Options().apply { inSampleSize = sampel }
    ) ?: return false

    val skala = SISI_MAKS.toFloat() / maxOf(gambar.width, gambar.height)
    val hasil = if (skala < 1f) {
        Bitmap.createScaledBitmap(
            gambar,
            (gambar.width * skala).toInt().coerceAtLeast(1),
            (gambar.height * skala).toInt().coerceAtLeast(1),
            true
        )
    } else {
        gambar
    }

    val sukses = hasil.compress(Bitmap.CompressFormat.JPEG, KUALITAS, output)
    if (hasil !== gambar) hasil.recycle()
    gambar.recycle()
    return sukses
}

private const val SISI_MAKS = 1600
private const val KUALITAS = 80
