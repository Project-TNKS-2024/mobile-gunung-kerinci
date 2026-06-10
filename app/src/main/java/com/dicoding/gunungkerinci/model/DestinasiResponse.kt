package com.dicoding.gunungkerinci.model

data class DestinasiResponse(
    val success: Boolean,
    val message: String,
    val data: List<Destinasi>,
    val errors: Any?
)

data class Destinasi(
    val id: Int,
    val nama: String,
    val status: Int,
    val statusGunung: Int,
    val kategori: String,
    val lokasi: String,
    val detail: String,
    val sop: String,
    val status_label: String,
    val status_gunung_label: String,
    val gambar_destinasi: List<GambarDestinasi>,
    val gates: List<Gate>
)

data class GambarDestinasi(
    val id: Int,
    val src: String,
    val nama: String,
    val detail: String,
    val id_destinasi: Int
)

data class Gate(
    val id: Int,
    val nama: String,
    val status: Int,
    val id_destinasi: Int,
    val max_pendaki_hari: Int,
    val min_pendaki_booking: Int,
    val lokasi: String,
    val lokasi_maps: String?,
    val detail: String?,
    val qris: String
)