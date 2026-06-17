package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class GantiPasswordRequest(

    @SerializedName("password_baru")
    val password_baru: String,

    @SerializedName("password_baru_confirmation")
    val password_baru_confirmation : String
)