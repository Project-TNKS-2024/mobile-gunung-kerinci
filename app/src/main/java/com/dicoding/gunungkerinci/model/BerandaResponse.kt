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
    @SerializedName("total_pendaki_wna") val totalPendakiWna: Int? = null,
    @SerializedName("ringkasan_pendaki") val ringkasanPendaki: RingkasanPendaki? = null,
    @SerializedName("status_gunung") val statusGunung: StatusGunungBeranda? = null,
    @SerializedName("ringkasan_diperbarui_pada") val diperbaruiPada: String? = null,
    @SerializedName("cuaca_diamati_pada") val cuacaDiamatiPada: Long? = null
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


data class RingkasanPendaki(
    @SerializedName("sedang_mendaki") val sedangMendaki: Int? = null,
    @SerializedName("total_tercatat") val totalTercatat: Int? = null,
    @SerializedName("cakupan") val cakupan: String? = null
)

data class StatusGunungBeranda(
    @SerializedName("kode") val kode: Int? = null,
    @SerializedName("label") val label: String? = null
)
