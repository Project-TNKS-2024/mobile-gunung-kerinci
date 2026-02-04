package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class CountryResponse (
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("data")
    val data: List<Country>
)