package com.dicoding.gunungkerinci.model

data class ProfileResponse (
    val data: ProfileData
)

data class ProfileData(
    val id: String,
    val email: String?,
    val first_name: String?,
    val last_name: String?,
    val kenegaraan: String?,
    val no_hp: String?,
    val jenis_kelamin: String?,
    val tanggal_lahir: String?,
    val nik: String?,
    val provinsi: String?,
    val kabupaten: String?,
    val kec: String?,
    val dataNegara: DataNegara?,
    val dataProvinsi: DataProvinsi?,
    val dataKabupaten: DataKabupaten?,
    val dataKecamatan: DataKecamatan?
)

data class DataNegara(val name: String?, val flag: String?, val code: String?, val dial_code: String?)
data class DataProvinsi(val id: Int?, val name: String?)
data class DataKabupaten(val id: Int?, val name: String?)
data class DataKecamatan(val id: Int?, val name: String?)