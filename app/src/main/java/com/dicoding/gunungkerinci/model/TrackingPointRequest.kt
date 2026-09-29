package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/** Satu titik posisi. Dipakai endpoint tunggal (/gps) dan batch (/gps/batch). */
data class TrackingPointRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("altitude") val altitude: Double? = null,
    @SerializedName("accuracy") val accuracy: Double? = null,
    @SerializedName("battery_level") val batteryLevel: Int? = null,
    @SerializedName("recorded_at") val recordedAt: String? = null
)

data class TrackingBatchRequest(
    @SerializedName("positions") val positions: List<TrackingPointRequest>
)

/** data dari respons /gps (201). */
data class TrackingGpsData(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("server_time") val serverTime: String? = null
)

/** data dari respons /gps/batch (201). */
data class TrackingBatchData(
    @SerializedName("inserted") val inserted: Int? = null,
    @SerializedName("server_time") val serverTime: String? = null
)
