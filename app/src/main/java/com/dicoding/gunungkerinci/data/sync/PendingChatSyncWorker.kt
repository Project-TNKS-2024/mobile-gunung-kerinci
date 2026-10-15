package com.dicoding.gunungkerinci.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dicoding.gunungkerinci.data.local.TnksDatabase
import com.dicoding.gunungkerinci.data.local.sos.PendingChatEntity
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.pref.UserPreference
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Kirim antrean pesan chat darurat yang tersimpan saat offline.
 * Tidak ada batas percobaan: berhentinya cuma kalau terkirim atau ditolak server.
 */
class PendingChatSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = TnksDatabase.getInstance(applicationContext).pendingChatDao()
        val antrean = dao.getPendingSync()
        // Antrean kosong: tidak ada kerjaan, sudahi sebelum cek login.
        if (antrean.isEmpty()) return Result.success()

        val token = UserPreference(applicationContext).getToken().orEmpty()
        if (token.isBlank()) return Result.retry() // belum login, coba lagi nanti

        val api = ApiConfig.getApiService(applicationContext)
        var masihAdaSisa = false

        for (chat in antrean) {
            when (kirim(api, token, chat)) {
                is Hasil.Sukses -> {
                    dao.delete(chat.localId)
                    bersihkanFile(chat)
                }
                is Hasil.Sementara -> {
                    dao.markFailed(chat.localId)
                    masihAdaSisa = true
                }
                is Hasil.Permanen -> {
                    dao.delete(chat.localId)
                    bersihkanFile(chat)
                }
            }
        }

        return if (masihAdaSisa) Result.retry() else Result.success()
    }

    private suspend fun kirim(api: ApiService, token: String, chat: PendingChatEntity): Hasil = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = if (chat.type == TYPE_IMAGE) {
            val file = File(chat.imagePath.orEmpty())
            if (!file.exists()) return@runCatching Hasil.Permanen
            val mime = if (file.name.lowercase().endsWith(".png")) "image/png" else "image/jpeg"
            val part = MultipartBody.Part.createFormData(
                "image", file.name, file.asRequestBody(mime.toMediaTypeOrNull())
            )
            api.sendSosChat(
                bearer, chat.sosId,
                type = "image".toRequestBody(TEXT),
                content = chat.content?.toRequestBody(TEXT),
                image = part
            )
        } else {
            api.sendSosChat(
                bearer, chat.sosId,
                type = "text".toRequestBody(TEXT),
                content = chat.content.orEmpty().toRequestBody(TEXT),
                image = null
            )
        }
        klasifikasi(response.isSuccessful, response.code())
    }.getOrElse { error ->
        if (error is java.io.IOException) Hasil.Sementara else Hasil.Permanen
    }

    private fun klasifikasi(success: Boolean, code: Int): Hasil = when {
        success -> Hasil.Sukses
        code == HTTP_TOO_MANY_REQUESTS -> Hasil.Sementara // rate limit, coba lagi nanti
        code == HTTP_FORBIDDEN || code == HTTP_NOT_FOUND || code == HTTP_UNPROCESSABLE ->
            Hasil.Permanen
        else -> Hasil.Sementara // 5xx dan lainnya
    }

    /** Hapus file gambar yang sudah tidak dipakai. */
    private fun bersihkanFile(chat: PendingChatEntity) {
        if (chat.type == TYPE_IMAGE) {
            runCatching { File(chat.imagePath.orEmpty()).delete() }
        }
    }

    private sealed interface Hasil {
        data object Sukses : Hasil
        data object Sementara : Hasil
        data object Permanen : Hasil
    }

    private companion object {
        const val TYPE_IMAGE = "image"
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_UNPROCESSABLE = 422
        val TEXT = "text/plain".toMediaTypeOrNull()
    }
}
