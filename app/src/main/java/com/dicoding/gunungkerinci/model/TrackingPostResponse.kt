package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class TrackingPost(
    @SerializedName("id") val id: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("urutan") val urutan: Int,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("altitude") val altitude: Int? = null,
    @SerializedName("radius_meter") val radiusMeter: Int? = null
)

typealias TrackingPostResponse = BaseResponse<List<TrackingPost>>
