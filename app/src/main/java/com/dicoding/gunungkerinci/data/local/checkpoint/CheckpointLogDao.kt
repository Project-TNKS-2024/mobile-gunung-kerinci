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

    @Query("SELECT COUNT(*) FROM checkpoint_logs WHERE bookingId = :bookingId AND pendakiId = :pendakiId AND postId = :postId AND phase = :phase AND syncStatus != 'REJECTED' AND retryCount < 99")
    suspend fun countExistingCheckIn(bookingId: String, pendakiId: String, postId: Int, phase: String): Int

    /**
     * Ambil ID log di server untuk satu pos + fase + pendaki.
     * `serverId` diisi saat kirim berhasil, dan dipakai untuk membatalkan kehadiran.
     */
    @Query("SELECT serverId FROM checkpoint_logs WHERE bookingId = :bookingId AND pendakiId = :pendakiId AND postId = :postId AND phase = :phase AND serverId IS NOT NULL ORDER BY checkedAt DESC LIMIT 1")
    suspend fun getServerId(bookingId: String, pendakiId: String, postId: Int, phase: String): Int?

    /** Ambil satu catatan lokal — dipakai memeriksa status sebelum membatalkan kehadiran. */
    @Query("SELECT * FROM checkpoint_logs WHERE bookingId = :bookingId AND pendakiId = :pendakiId AND postId = :postId AND phase = :phase ORDER BY checkedAt DESC LIMIT 1")
    suspend fun getLog(bookingId: String, pendakiId: String, postId: Int, phase: String): CheckpointLogEntity?

    /** Semua catatan lokal satu booking — dipakai overlay "menunggu kirim" di daftar anggota. */
    @Query("SELECT * FROM checkpoint_logs WHERE bookingId = :bookingId")
    suspend fun getByBooking(bookingId: String): List<CheckpointLogEntity>

    @Query("DELETE FROM checkpoint_logs WHERE bookingId = :bookingId AND pendakiId = :pendakiId AND syncStatus = 'SENT' AND (serverId IS NULL OR serverId NOT IN (:serverIds))")
    suspend fun removeStaleServerLogs(bookingId: String, pendakiId: String, serverIds: List<Int>)

    /** Hapus catatan lokal satu pos + fase setelah kehadirannya dibatalkan di server. */
    @Query("DELETE FROM checkpoint_logs WHERE bookingId = :bookingId AND pendakiId = :pendakiId AND postId = :postId AND phase = :phase")
    suspend fun deleteCheckIn(bookingId: String, pendakiId: String, postId: Int, phase: String)

    @Query("UPDATE checkpoint_logs SET syncStatus = :status WHERE localId = :localId")
    suspend fun updateStatus(localId: String, status: String)

    @Query("UPDATE checkpoint_logs SET clientEventId = :clientEventId WHERE localId = :localId")
    suspend fun setClientEventId(localId: String, clientEventId: String)

    @Query("UPDATE checkpoint_logs SET syncStatus = :status, serverId = :serverId WHERE localId = :localId")
    suspend fun markSent(localId: String, serverId: Int?, status: String = "SENT")

    @Query("UPDATE checkpoint_logs SET syncStatus = :status, retryCount = retryCount + 1 WHERE localId = :localId")
    suspend fun markFailed(localId: String, status: String = "FAILED")

    @Query("UPDATE checkpoint_logs SET syncStatus = 'REJECTED', retryCount = 99 WHERE localId = :localId")
    suspend fun markPermanentFailure(localId: String)

    /**
     * Reset baris yang tertinggal di status SYNCING (worker mati di tengah kirim)
     * kembali ke PENDING agar ikut dikirim ulang.
     */
    @Query("UPDATE checkpoint_logs SET syncStatus = 'PENDING' WHERE syncStatus = 'SYNCING'")
    suspend fun resetStuckSyncing()

    /**
     * Buka percobaan baru untuk catatan yang sudah menyerah karena habis percobaan.
     */
    @Query("UPDATE checkpoint_logs SET syncStatus = 'PENDING', retryCount = 0 WHERE bookingId = :bookingId AND syncStatus = 'FAILED' AND retryCount < 99")
    suspend fun resetRetriableFailures(bookingId: String): Int

    /** Catatan naik di satu pos (semua fase) — dipakai cek dependensi summit. */
    @Query("SELECT * FROM checkpoint_logs WHERE bookingId = :bookingId AND pendakiId = :pendakiId AND phase = 'naik' ORDER BY checkedAt ASC")
    suspend fun getAscentLogs(bookingId: String, pendakiId: String): List<CheckpointLogEntity>

    /** Apakah pendaki sudah punya log naik puncak (server terkirim) untuk booking ini. */
    @Query("SELECT COUNT(*) FROM checkpoint_logs WHERE bookingId = :bookingId AND pendakiId = :pendakiId AND isSummit = 1 AND phase = 'naik' AND syncStatus = 'SENT'")
    suspend fun countSentSummit(bookingId: String, pendakiId: String): Int
}
