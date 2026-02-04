package com.dicoding.gunungkerinci.model

data class LoginResponse (
    val success: Boolean,
    val message: String?,
    val data: LoginData?,
    val errors: Any?
)

data class LoginData(
    val user: User,
    val token: String
)