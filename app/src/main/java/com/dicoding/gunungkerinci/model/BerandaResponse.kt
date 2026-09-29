package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/**
 * Respons `GET /api/beranda` (lihat `HomeController::beranda` di backend).
 *
 * `cuaca` adalah respons mentah WeatherAPI, jadi bisa null kalau pengambilan cuaca gagal.
 */
data class BerandaResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: BerandaData? = null,
    @SerializedName("errors") val errors: Any? = null
)

data class BerandaData(
    @SerializedName("cuaca") val cuaca: Cuaca? = null,
    @SerializedName("total_mendaki") val totalMendaki: Int? = null,
    @SerializedName("total_pendaki") val totalPendaki: Int? = null,
    @SerializedName("total_pendaki_wni") val totalPendakiWni: Int? = null,
    @SerializedName("total_pendaki_wna") val totalPendakiWna: Int? = null
)

data class Cuaca(
    @SerializedName("location") val location: CuacaLocation? = null,
    @SerializedName("current") val current: CuacaCurrent? = null
)

data class CuacaLocation(
    @SerializedName("name") val name: String? = null,
    @SerializedName("region") val region: String? = null
)

data class CuacaCurrent(
    @SerializedName("temp_c") val tempC: Double? = null,
    @SerializedName("condition") val condition: CuacaCondition? = null
)

data class CuacaCondition(
    @SerializedName("text") val text: String? = null,
    @SerializedName("icon") val icon: String? = null
)
