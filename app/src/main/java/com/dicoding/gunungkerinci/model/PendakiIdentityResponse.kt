package com.dicoding.gunungkerinci.model

data class PendakiIdentityResponse (
    val success: Boolean,
    val message: String,
    val data: PendakiIdentityData?
)

data class PendakiIdentityData (
    val id_user: Int?,
    val id_bio: String?,
    val status_verifikasi: String?,
    val verified_at: String?,
    val pendaki_ids: List<Any>?
)