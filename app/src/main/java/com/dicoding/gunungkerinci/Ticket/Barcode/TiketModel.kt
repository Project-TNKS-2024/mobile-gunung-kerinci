package com.dicoding.gunungkerinci.Ticket.Barcode

import android.graphics.Bitmap

/**
 * Data kartu tiket individu pada halaman Barcode. QR memakai kode booking yang
 * sama untuk semua anggota; petugas memilih orang setelah membuka detail booking.
 */
data class TiketModel(
    val nama: String,
    val statusPendaki: String? = null,
    val noSeri: String,
    val gerbangMasuk: String,
    val gerbangKeluar: String,
    val tanggalMasuk: String,
    val tanggalKeluar: String,
    val kodePemesanan: String,
    val qrBitmap: Bitmap?
)
