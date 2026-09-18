package com.dicoding.gunungkerinci.ui.pelacakan_jejak

/**
 * Model UI layar Jejak.
 *
 * Semua kelas di file ini dipetakan dari respons API (lihat [TicketUiMapper],
 * [TrackingPostUiMapper], [TrackingProgressUiMapper]) — bukan data statis.
 */

internal data class JejakTicketUi(
    val title: String,
    val purchaseLabel: String,
    val purchaseDate: String,
    val status: String,
    val statusType: TicketStatusType,
    val masukDate: String,
    val keluarDate: String,
    val pendakiCount: String,
    val totalPayment: String,
    val bookingId: String? = null,
    val primaryAction: String
)

internal enum class TicketStatusType { Ongoing, WaitingPayment, Done }

internal data class JejakDayUi(
    val title: String,
    val date: String,
    val selected: Boolean
)

internal data class JejakCheckpointUi(
    val times: List<String>,
    val name: String,
    val description: String,
    val state: CheckpointState,
    val postId: Int? = null,
    val order: Int? = null,
    val altitude: Int? = null,
    /** Nama anggota yang sudah check-in di pos ini — dipakai avatar bertumpuk di timeline. */
    val memberNames: List<String> = emptyList(),
    /** Contoh: "checkpoint pada 10:30 wib". Null kalau pos belum di-check-in. */
    val checkedAtLabel: String? = null
)

internal enum class CheckpointState { Completed, Current, Upcoming }

/**
 * Status gerbang (gate) check-in GPS — penegakan aturan keselamatan:
 * check-in hanya boleh saat GPS aktif DAN pendaki berada dalam radius pos.
 *
 * Dipakai untuk mengunci tombol check-in (lihat PelacakanJejakLocationDetailScreen).
 */
internal sealed interface GpsGateState {

    /** Belum ada hasil deteksi (layar baru dibuka / sedang memeriksa posisi). */
    data object Unknown : GpsGateState

    /** GPS mati, izin ditolak, atau lokasi tidak tersedia → check-in terkunci. */
    data object GpsOff : GpsGateState

    /** GPS aktif, tetapi pendaki di luar radius pos terdekat → check-in terkunci. */
    data class LuarRadius(
        val postName: String,
        val distanceMeters: Double,
        val radiusMeter: Int
    ) : GpsGateState

    /** GPS aktif + berada dalam radius pos terdekat → check-in boleh dilakukan. */
    data class DalamRadius(
        val postName: String,
        val distanceMeters: Double,
        val radiusMeter: Int
    ) : GpsGateState
}

/** Anggota tim pada satu pos — dipakai di section "Cek Anggota" layar Location Detail. */
internal data class JejakMemberUi(
    val pendakiId: String,
    val name: String,
    val checked: Boolean
)
