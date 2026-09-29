package com.dicoding.gunungkerinci.data.local.checkpoint

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CheckpointLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: CheckpointLogEntity)

    @Query("SELECT * FROM checkpoint_logs WHERE syncStatus IN (:pending, :failed) ORDER BY checkedAt ASC")
    suspend fun getPendingSync(
        pending: String = "PENDING",
        failed: String = "FAILED"
    ): List<CheckpointLogEntity>

    @Query("SELECT * FROM checkpoint_logs WHERE bookingId = :bookingId ORDER BY checkedAt DESC LIMIT 1")
    suspend fun getLastCheckpoint(bookingId: String): CheckpointLogEntity?

    @Query("SELECT COUNT(*) FROM checkpoint_logs WHERE bookingId = :bookingId AND pendakiId = :pendakiId AND postId = :postId")
    suspend fun countExistingCheckIn(bookingId: String, pendakiId: String, postId: Int): Int

    @Query("UPDATE checkpoint_logs SET syncStatus = :status WHERE localId = :localId")
    suspend fun updateStatus(localId: String, status: String)

    @Query("UPDATE checkpoint_logs SET syncStatus = :status, serverId = :serverId WHERE localId = :localId")
    suspend fun markSent(localId: String, serverId: Int?, status: String = "SENT")

    @Query("UPDATE checkpoint_logs SET syncStatus = :status, retryCount = retryCount + 1 WHERE localId = :localId")
    suspend fun markFailed(localId: String, status: String = "FAILED")

    @Query("UPDATE checkpoint_logs SET syncStatus = 'FAILED', retryCount = 99 WHERE localId = :localId")
    suspend fun markPermanentFailure(localId: String)
}
