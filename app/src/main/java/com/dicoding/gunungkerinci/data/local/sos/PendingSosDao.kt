package com.dicoding.gunungkerinci.data.local.sos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingSosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sos: PendingSosEntity)

    @Query("SELECT * FROM pending_sos WHERE syncStatus IN (:pending, :failed) ORDER BY createdAt ASC")
    suspend fun getPendingSync(
        pending: String = "PENDING",
        failed: String = "FAILED"
    ): List<PendingSosEntity>

    @Query("UPDATE pending_sos SET syncStatus = :status WHERE localId = :localId")
    suspend fun updateStatus(localId: String, status: String)

    @Query("UPDATE pending_sos SET syncStatus = 'SENT' WHERE localId = :localId")
    suspend fun markSent(localId: String)

    /** Buang baris yang sudah terkirim supaya tabelnya tidak menumpuk. */
    @Query("DELETE FROM pending_sos WHERE syncStatus = 'SENT'")
    suspend fun deleteSent()

    @Query("UPDATE pending_sos SET syncStatus = :status, retryCount = retryCount + 1 WHERE localId = :localId")
    suspend fun markFailed(localId: String, status: String = "FAILED")

    /**
     * Server menolak permanen — ubah statusnya supaya baris ini
     * tidak ikut terambil oleh [getPendingSync].
     */
    @Query("UPDATE pending_sos SET syncStatus = 'REJECTED' WHERE localId = :localId")
    suspend fun markPermanentFailure(localId: String)

    /**
     * Reset baris yang tertinggal di status SYNCING (worker mati di tengah kirim)
     * kembali ke PENDING agar SOS ikut dikirim ulang — lihat A13.
     */
    @Query("UPDATE pending_sos SET syncStatus = 'PENDING' WHERE syncStatus = 'SYNCING'")
    suspend fun resetStuckSyncing()
}
