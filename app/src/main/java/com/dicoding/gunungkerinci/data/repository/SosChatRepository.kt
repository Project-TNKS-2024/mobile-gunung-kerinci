package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.data.local.sos.PendingChatDao
import com.dicoding.gunungkerinci.data.local.sos.PendingChatEntity
import com.dicoding.gunungkerinci.model.SosMessagesData
import com.dicoding.gunungkerinci.model.SosSendMessageResponse
import com.dicoding.gunungkerinci.model.SosSentMessage
import com.dicoding.gunungkerinci.network.ApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.util.UUID

/**
 * Chat SOS (multipart): kirim teks/gambar & ambil riwayat pesan.
 * Endpoint: POST/GET /api/sos/chat/{sos_id}/...
 */
internal class SosChatRepository(
    private val api: ApiService,
    private val pendingChatDao: PendingChatDao? = null
) {

    suspend fun sendText(token: String, sosId: Int, text: String): Result<SosSentMessage> = runCatching {
        val response = api.sendSosChat(
            bearerOf(token),
            sosId,
            type = "text".toRequestBody(TEXT),
            content = text.toRequestBody(TEXT),
            image = null
        )
        parseSent(response)
    }

    suspend fun sendImage(
        token: String,
        sosId: Int,
        image: MultipartBody.Part,
        caption: String? = null
    ): Result<SosSentMessage> = runCatching {
        val response = api.sendSosChat(
            bearerOf(token),
            sosId,
            type = "image".toRequestBody(TEXT),
            content = caption?.toRequestBody(TEXT),
            image = image
        )
        parseSent(response)
    }

    suspend fun getMessages(token: String, sosId: Int, page: Int? = null): Result<SosMessagesData> = runCatching {
        val response = api.getSosMessages(bearerOf(token), sosId, page)
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.message ?: messageFor(response.code()))
        }
    }

    /**
     * Antrean lokal pesan chat saat offline (US-12): pesan disimpan ke Room
     * (`pending_sos_chats`) dan dikirim ulang oleh PendingChatSyncWorker saat
     * jaringan tersedia. Hanya dipakai saat SOS sudah aktif di server (sosId valid).
     */
    suspend fun savePendingChat(sosId: Int, type: String, content: String?, imagePath: String?): Boolean {
        val dao = pendingChatDao ?: return false
        return runCatching {
            dao.upsert(
                PendingChatEntity(
                    localId = UUID.randomUUID().toString(),
                    sosId = sosId,
                    type = type,
                    content = content,
                    imagePath = imagePath,
                    createdAt = System.currentTimeMillis()
                )
            )
            true
        }.getOrDefault(false)
    }

    suspend fun getPendingChats(sosId: Int): List<PendingChatEntity> =
        pendingChatDao?.getPendingForSos(sosId) ?: emptyList()

    suspend fun deletePendingChat(localId: String) {
        pendingChatDao?.delete(localId)
    }

    /** Kirim gambar langsung dari file (dipakai ViewModel & worker). */
    suspend fun sendImageFile(token: String, sosId: Int, file: File, caption: String? = null): Result<SosSentMessage> = runCatching {
        val mime = if (file.name.lowercase().endsWith(".png")) "image/png" else "image/jpeg"
        val part = MultipartBody.Part.createFormData(
            "image", file.name, file.asRequestBody(mime.toMediaType())
        )
        sendImage(token, sosId, part, caption).getOrThrow()
    }

    private fun parseSent(response: Response<SosSendMessageResponse>): SosSentMessage {
        val body = response.body()
        return if (response.isSuccessful && body?.success == true && body.data != null) {
            body.data
        } else {
            throw Exception(body?.message ?: messageFor(response.code()))
        }
    }

    private fun messageFor(code: Int) = when (code) {
        403 -> "Tidak memiliki akses ke SOS ini"
        404 -> "SOS tidak ditemukan"
        422 -> "Pesan tidak valid"
        else -> "Gagal mengirim pesan ($code)"
    }

    private fun bearerOf(token: String) =
        if (token.startsWith("Bearer ")) token else "Bearer $token"

    private companion object {
        val TEXT = "text/plain".toMediaType()
    }
}
