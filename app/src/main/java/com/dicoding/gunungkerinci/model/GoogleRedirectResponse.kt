package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class GoogleRedirectResponse(
    @SerializedName("redirect_url")
    val redirect_url: String
)