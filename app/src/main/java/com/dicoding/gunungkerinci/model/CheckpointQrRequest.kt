package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

data class CheckpointQrRequest(
    @SerializedName("qr_code_value") val qrCodeValue: String,
    /**
     * UUID pendaki yang dicatat kehadirannya.
     * `null` = catat kehadiran diri sendiri. Diisi = ketua tim mencatat anggota
     * (server menolak 403 bila pemanggil bukan pemilik booking).
     */
    @SerializedName("pendaki_id") val pendakiId: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    /** Fase kunjungan "naik"/"turun" (wajib di kontrak v2). */
    @SerializedName("phase") val phase: String? = null,
    /** Identitas kejadian untuk retry idempoten (dibuat sekali di mobile). */
    @SerializedName("client_event_id") val clientEventId: String? = null,
    /** Waktu kejadian ISO-8601 (diperoleh dari jam perangkat, dikirim apa adanya). */
    @SerializedName("recorded_at") val recordedAt: String? = null
)
