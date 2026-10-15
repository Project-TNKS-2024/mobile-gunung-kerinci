package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class TrackingPost(
    @SerializedName("id") val id: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("urutan") val urutan: Int,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("altitude") val altitude: Int? = null,
    @SerializedName("radius_meter") val radiusMeter: Int? = null,
    /** Durasi dari pos INI ke pos berikutnya, bukan durasi menuju pos ini. */
    @SerializedName("estimated_time_minutes") val estimatedTimeMinutes: Int? = null,
    @SerializedName("info_is_dummy") val infoIsDummy: Boolean = false,
    @SerializedName("temperature_c") val temperatureC: Double? = null,
    @SerializedName("distance_to_summit_m") val distanceToSummitM: Int? = null
)

typealias TrackingPostResponse = BaseResponse<List<TrackingPost>>
