package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName
import com.google.gson.Gson
import retrofit2.Response

/**
 * Kesalahan checkpoint yang membawa kode mesin backend, agar pemanggil dapat
 * membedakan konflik fase/dependensi dari kegagalan jaringan.
 */
class CheckpointApiException(
    val code: String?,
    message: String,
    val checkpointLogId: Int? = null,
    val checkedAt: String? = null,
    val visitKey: String? = null,
    val summitPostId: Int? = null,
    val httpStatus: Int = 0
) : Exception(message)

/** Bentuk amplop kesalahan `ApiResponse::error` untuk di-parse. */
internal data class CheckpointErrorEnvelope(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("errors") val errors: CheckpointV2Error?
)

/** Retrofit keeps non-2xx JSON in errorBody(), never body(). */
internal fun Response<*>.checkpointException(fallback: String): CheckpointApiException {
    val envelope = runCatching {
        val json = if (isSuccessful) Gson().toJson(body()) else errorBody()?.string()
        json?.let { Gson().fromJson(it, CheckpointErrorEnvelope::class.java) }
    }.getOrNull()
    val error = envelope?.errors
    return CheckpointApiException(
        code = error?.code,
        message = envelope?.message ?: "$fallback (${code()})",
        checkpointLogId = error?.checkpointLogId,
        checkedAt = error?.checkedAt,
        visitKey = error?.visitKey,
        summitPostId = error?.summitPostId,
        httpStatus = code()
    )
}
