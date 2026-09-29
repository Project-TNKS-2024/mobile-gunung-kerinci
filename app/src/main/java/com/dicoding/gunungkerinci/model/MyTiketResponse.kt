package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class MyTiketResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<MyTiketItem>? = null,
    @SerializedName("errors") val errors: Any? = null
)
data class MyTiketItem(
    @SerializedName("id") val id: String,
    @SerializedName("status_booking") val statusBooking: Int? = null,
    @SerializedName("tanggal_masuk") val tanggalMasuk: String? = null,
    @SerializedName("tanggal_keluar") val tanggalKeluar: String? = null,
    @SerializedName("total_pendaki_wni") val totalPendakiWni: Int? = null,
    @SerializedName("total_pendaki_wna") val totalPendakiWna: Int? = null,
    @SerializedName("total_pembayaran") val totalPembayaran: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("gate_masuk") val gateMasuk: MyTiketGate? = null,
    @SerializedName("gate_keluar") val gateKeluar: MyTiketGate? = null,
    @SerializedName("pendakis") val pendakis: List<MyTiketPendaki>? = null
)
data class MyTiketGate(
    @SerializedName("id") val id: Int,
    @SerializedName("nama") val nama: String? = null
)
data class MyTiketPendaki(
    @SerializedName("id") val id: String,
    @SerializedName("id_bio") val idBio: String? = null,
    @SerializedName("biodata") val biodata: MyTiketBiodata? = null
)
data class MyTiketBiodata(
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null
)