package com.dicoding.gunungkerinci.model

data class DetailDestinasiResponse (
    val success: Boolean,
    val message: String,
    val data: Destinasi,
    val errors: Any?
)

data class DestinasiDetail(
    val id: Int,
    val nama: String,
    val detail: String?,
    val lokasi: String?,
    val status_label: String?,
    val status_gunung_label: String?,
    val gambar_destinasi: List<GambarDestinasi>?
)