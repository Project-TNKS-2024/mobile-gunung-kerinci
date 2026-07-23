package com.dicoding.gunungkerinci.model

data class CreateBookingRequest(
    val date_start: String,
    val date_end: String,
    val wni: Int,
    val wna: Int,
    val jenis_tiket: Int,
    val gerbang_masuk: Int,
    val gerbang_keluar: Int
)