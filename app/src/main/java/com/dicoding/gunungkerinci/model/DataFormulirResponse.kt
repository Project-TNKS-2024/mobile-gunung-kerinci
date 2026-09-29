package com.dicoding.gunungkerinci.model

data class DataFormulirResponse(
    val success: Boolean,
    val message: String,
    val data: DataFormulirData
)

data class DataFormulirData(
    val booking: BookingFormulir
)

data class BookingFormulir(
    val id: String,
    val destinasi: DestinasiFormulir,
    val pendakis: List<PendakiFormulir> = emptyList()
)

data class DestinasiFormulir(
    val id: Int,
    val nama: String,
    val sop: String
)

data class PendakiFormulir(
    val id: String,
    val booking_id: String,
    val id_bio: String,
    val usia: Int,
    val biodata: BiodataPendakiFormulir,
    val tagihan: Int? = null
)

data class BiodataPendakiFormulir(
    val id: String,
    val first_name: String?,
    val last_name: String?,
    val no_hp: String?,
    val no_hp_darurat: String?,
    val tanggal_lahir: String?,
    val dataNegara: NegaraPendakiFormulir?
)

data class NegaraPendakiFormulir(
    val name: String?,
    val flag: String?,
    val code: String?,
    val dial_code: String?
)