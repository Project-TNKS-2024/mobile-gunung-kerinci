package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class ResetPasswordRequest (
    @SerializedName("token")
    val token: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("password_confirmation")
    val passwordConfirmation: String
)