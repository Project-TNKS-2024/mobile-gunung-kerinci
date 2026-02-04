package com.dicoding.gunungkerinci.model

data class ProvinsiResponse(
    val success: Boolean,
    val data: List<Provinsi>
)

data class Provinsi(
    val id: Int,
    val name: String,
    val code: String
)