package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/** Respons POST /api/sos/disaster-report. */
data class DisasterReportResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: DisasterReportData?,
    @SerializedName("errors") val errors: Any?
)

data class DisasterReportData(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String?,
    @SerializedName("created_at") val createdAt: String?
)

/** Respons GET /api/sos/disaster-reports. */
data class DisasterReportsListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<DisasterReportItem>?,
    @SerializedName("errors") val errors: Any?
)

data class DisasterReportItem(
    @SerializedName("id") val id: Int,
    @SerializedName("potensi_bencana") val potensiBencana: String?,
    @SerializedName("lokasi") val lokasi: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("created_at") val createdAt: String?
)
