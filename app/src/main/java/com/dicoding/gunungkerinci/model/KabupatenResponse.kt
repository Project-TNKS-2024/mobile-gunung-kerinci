package com.dicoding.gunungkerinci.model

data class KabupatenResponse(
    val success: Boolean,
    val data: Map<String, Kabupaten>
)

data class Kabupaten(
    val id: Int,
    val name: String,
    val type: String,
    val provinsi_id: Int
)