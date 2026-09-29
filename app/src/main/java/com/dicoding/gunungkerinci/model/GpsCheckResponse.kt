package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class GpsCheckResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: GpsCheckData?,
    @SerializedName("errors") val errors: Any?
)

data class GpsCheckData(
    @SerializedName("nearest_post") val nearestPost: GpsNearestPost,
    @SerializedName("all_posts") val allPosts: List<GpsPostDistance>
)

data class GpsNearestPost(
    @SerializedName("id") val id: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("within_radius") val withinRadius: Boolean,
    @SerializedName("radius_meter") val radiusMeter: Int
)

data class GpsPostDistance(
    @SerializedName("id") val id: Int,
    @SerializedName("nama") val nama: String,
    @SerializedName("urutan") val urutan: Int,
    @SerializedName("distance_meters") val distanceMeters: Double,
    @SerializedName("within_radius") val withinRadius: Boolean
)
