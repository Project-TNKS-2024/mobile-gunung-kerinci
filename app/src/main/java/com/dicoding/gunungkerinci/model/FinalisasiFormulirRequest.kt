package com.dicoding.gunungkerinci.model

data class FinalisasiFormulirRequest(
    val id_booking: String,
    val action: String,
    val barangWajib: Boolean,
    val formulir: List<FormulirItem>,
    val barang_bawaan: List<BarangBawaan>
)