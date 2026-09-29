package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class GpsCheckRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Double? = null
)
