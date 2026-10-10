package com.dicoding.gunungkerinci.model

data class SimpanFormulirRequest(
    val id_booking: String,
    val action: String,
    val formulir: List<FormulirItem>,
    val barang_bawaan: List<BarangBawaan>
)

data class FormulirItem(
    val id_pendaki: String,
    val kode_bio: String,
    val no_hp_darurat: String
)

data class BarangBawaan(
    val nama_barang: String,
    val jumlah: Int
)