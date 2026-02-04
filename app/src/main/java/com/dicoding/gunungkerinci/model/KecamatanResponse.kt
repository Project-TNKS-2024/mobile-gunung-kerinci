package com.dicoding.gunungkerinci.model

data class KecamatanResponse(
    val success: Boolean,
    val data: Map<String, Kecamatan>
)

data class Kecamatan(
    val id: Int,
    val name: String,
    val kabupaten_id: Int
)