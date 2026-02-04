package com.dicoding.gunungkerinci.model

data class ProfileResponse (
    val data: ProfileData
)

data class ProfileData(
    val id: Int,
    val email: String,
    val first_name: String,
    val last_name: String,
    val nationality: String,
    val address: String,
    val gender: String,
    val birth_date: String,
    val identity_number: String,
    val phone: String
)