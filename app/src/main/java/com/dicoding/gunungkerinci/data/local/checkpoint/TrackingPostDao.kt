package com.dicoding.gunungkerinci.data.local.checkpoint

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackingPostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(posts: List<TrackingPostEntity>)

    @Query("SELECT * FROM tracking_posts WHERE gateId = :gateId ORDER BY `order` ASC")
    suspend fun getByGate(gateId: Int): List<TrackingPostEntity>

    @Query("SELECT * FROM tracking_posts WHERE qrCodeValue = :qrCodeValue LIMIT 1")
    suspend fun getByQrCode(qrCodeValue: String): TrackingPostEntity?

    @Query("SELECT * FROM tracking_posts WHERE gateId = :gateId AND status = :status ORDER BY `order` ASC")
    suspend fun getActivePosts(gateId: Int, status: String = "active"): List<TrackingPostEntity>
}
