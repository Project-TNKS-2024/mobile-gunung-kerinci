package com.dicoding.gunungkerinci.data.local.checkpoint

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracking_posts")
data class TrackingPostEntity(
    @PrimaryKey val id: Int,
    val gateId: Int,
    val name: String,
    val order: Int,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val radiusMeter: Double,
    val qrCodeValue: String?,
    val status: String,
    val estimatedTimeMinutes: Int? = null,
    @ColumnInfo(defaultValue = "0") val infoIsDummy: Boolean = false,
    val temperatureC: Double? = null,
    val distanceToSummitM: Int? = null,
    val distanceToNextM: Int? = null,
    @ColumnInfo(defaultValue = "0") val isSummit: Boolean = false,
    val estimatedDescentTimeMinutes: Int? = null,
    val mapImageUrl: String? = null
)
