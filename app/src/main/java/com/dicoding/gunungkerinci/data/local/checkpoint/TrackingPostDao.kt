package com.dicoding.gunungkerinci.data.local.checkpoint

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TrackingPostDao {
    @Query("DELETE FROM tracking_posts WHERE gateId = :gateId AND id NOT IN (:ids)")
    suspend fun deleteMissing(gateId: Int, ids: List<Int>)

    @Transaction
    suspend fun replaceForGate(gateId: Int, posts: List<TrackingPostEntity>) {
        deleteMissing(gateId, posts.map { it.id })
        upsertAll(posts)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(posts: List<TrackingPostEntity>)

    @Query("SELECT * FROM tracking_posts WHERE gateId = :gateId ORDER BY `order` ASC")
    suspend fun getByGate(gateId: Int): List<TrackingPostEntity>

    @Query("SELECT * FROM tracking_posts WHERE qrCodeValue = :qrCodeValue LIMIT 1")
    suspend fun getByQrCode(qrCodeValue: String): TrackingPostEntity?

    @Query("SELECT * FROM tracking_posts WHERE gateId = :gateId AND status = :status ORDER BY `order` ASC")
    suspend fun getActivePosts(gateId: Int, status: String = "active"): List<TrackingPostEntity>
}
