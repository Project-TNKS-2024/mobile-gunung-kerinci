package com.dicoding.gunungkerinci.model

data class RegisterResponse (
    val success: Boolean,
    val message: String,
    val data: RegisterData?,
    val errors: Map<String, List<String>>?
)

data class RegisterData(
    val user: User,
    val token: String
)

data class User(
    val id: Int,
    val email: String,
    val gauth_type: String,
    val created_at: String,
    val updated_at: String
)