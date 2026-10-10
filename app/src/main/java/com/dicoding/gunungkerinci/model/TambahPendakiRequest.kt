package com.dicoding.gunungkerinci.model

data class TambahPendakiRequest (
    val booking: String,
    val code: String,
    val id: String? = null
)