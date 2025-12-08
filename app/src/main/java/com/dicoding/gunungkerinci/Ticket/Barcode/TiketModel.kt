package com.dicoding.gunungkerinci.Ticket.Barcode

data class TiketModel(
    val nama: String,
    val noSeri: String,
    val gerbangMasuk: String,
    val gerbangKeluar: String,
    val tanggalMasuk: String,
    val tanggalKeluar: String,
    val kodePemesanan: String,
    val qrImageRes: Int
)
