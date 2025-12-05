package com.dicoding.gunungkerinci.Laporan

data class LaporanModel(
    val nama: String,
    val waktu: String,
    val lokasiA: String,
    val lokasiB: String,
    val deskripsi: String,
    val foto: List<Int> //pakai resource dulu untuk dummy
)