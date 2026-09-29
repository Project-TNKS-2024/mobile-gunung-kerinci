package com.dicoding.gunungkerinci.data.local.sos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chat: PendingChatEntity)

    @Query("SELECT * FROM pending_sos_chats WHERE syncStatus IN (:pending, :failed) ORDER BY createdAt ASC")
    suspend fun getPendingSync(
        pending: String = "PENDING",
        failed: String = "FAILED"
    ): List<PendingChatEntity>

    @Query("SELECT * FROM pending_sos_chats WHERE sosId = :sosId AND syncStatus = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingForSos(sosId: Int): List<PendingChatEntity>

    @Query("UPDATE pending_sos_chats SET retryCount = retryCount + 1, syncStatus = 'FAILED' WHERE localId = :localId")
    suspend fun markFailed(localId: String)

    @Query("DELETE FROM pending_sos_chats WHERE localId = :localId")
    suspend fun delete(localId: String)
}
