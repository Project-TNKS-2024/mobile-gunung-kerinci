package com.dicoding.gunungkerinci.model

data class RegisterRequest (
    val email: String,
    val password: String,
    val password_confirmation: String
)