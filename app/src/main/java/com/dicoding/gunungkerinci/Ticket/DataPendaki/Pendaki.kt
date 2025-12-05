package com.dicoding.gunungkerinci.Ticket.DataPendaki

data class Pendaki(
    val nama: String,
    val idPendaki: String,
    val kewarganegaraan: String,
    val noIdentitas: String,
    val jenisKelamin: String,
    val tanggalLahir: String,
    val alamat: String,
    val noTelepon: String,
    val noDarurat: String,
    val status: String // "Ketua" atau "Anggota"
)