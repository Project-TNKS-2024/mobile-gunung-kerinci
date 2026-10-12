package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.data.local.warning.PeringatanDiniDao
import com.dicoding.gunungkerinci.data.local.warning.PeringatanDiniEntity
import com.dicoding.gunungkerinci.data.local.warning.PeringatanDiniMetaEntity
import com.dicoding.gunungkerinci.model.PeringatanDiniDto
import com.dicoding.gunungkerinci.network.ApiService
import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDini

internal class PeringatanDiniApiException(val code: Int, message: String) : Exception(message)

internal class PeringatanDiniRepository(
    private val api: ApiService,
    private val dao: PeringatanDiniDao? = null
) {

    suspend fun getActiveWarnings(token: String): Result<List<PeringatanDini>> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.getActivePeringatanDini(bearer)
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            body.data.orEmpty().map { it.toWarning() }
        } else {
            throw PeringatanDiniApiException(
                response.code(),
                body?.message ?: when (response.code()) {
                    401 -> "Sesi berakhir, silakan login ulang"
                    403 -> "Tidak ada pendakian aktif"
                    else -> "Gagal memuat peringatan (${response.code()})"
                }
            )
        }
    }

    // Isi cache, hanya dikembalikan kalau pemiliknya sama dengan ownerKey.
    suspend fun cachedFor(ownerKey: String): List<PeringatanDini> {
        if (ownerKey.isBlank()) return emptyList()
        val store = dao ?: return emptyList()
        val meta = store.getMeta() ?: return emptyList()
        if (meta.ownerKey != ownerKey) return emptyList()
        return store.getAll().map { it.toWarning() }
    }

    // Waktu sinkron terakhir cache, kalau memang milik ownerKey.
    suspend fun cachedAt(ownerKey: String): Long? {
        if (ownerKey.isBlank()) return null
        val store = dao ?: return null
        val meta = store.getMeta() ?: return null
        return if (meta.ownerKey == ownerKey) meta.lastSyncEpochMillis else null
    }

    // Ganti isi cache dengan data terbaru, sekaligus catat pemilik & waktu sync.
    suspend fun save(ownerKey: String, warnings: List<PeringatanDini>, atEpochMillis: Long) {
        if (ownerKey.isBlank()) return
        val store = dao ?: return
        store.clear()
        store.insertAll(warnings.map { it.toEntity() })
        store.setMeta(
            PeringatanDiniMetaEntity(ownerKey = ownerKey, lastSyncEpochMillis = atEpochMillis)
        )
    }
}

private fun PeringatanDiniDto.toWarning() = PeringatanDini(
    id = id,
    type = type,
    title = title ?: "(Tanpa judul)",
    description = description.orEmpty(),
    latitude = latitude,
    longitude = longitude,
    severity = severity ?: "low",
    createdAt = createdAt.orEmpty()
)

private fun PeringatanDiniEntity.toWarning() = PeringatanDini(
    id = id,
    type = type,
    title = title ?: "(Tanpa judul)",
    description = description.orEmpty(),
    latitude = latitude,
    longitude = longitude,
    severity = severity ?: "low",
    createdAt = createdAt.orEmpty()
)

private fun PeringatanDini.toEntity() = PeringatanDiniEntity(
    id = id,
    type = type,
    title = title,
    description = description,
    latitude = latitude,
    longitude = longitude,
    severity = severity,
    createdAt = createdAt
)
