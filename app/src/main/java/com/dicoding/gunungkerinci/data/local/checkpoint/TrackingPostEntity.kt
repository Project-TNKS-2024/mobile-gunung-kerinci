package com.dicoding.gunungkerinci.data.local.checkpoint

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
    val status: String
)
