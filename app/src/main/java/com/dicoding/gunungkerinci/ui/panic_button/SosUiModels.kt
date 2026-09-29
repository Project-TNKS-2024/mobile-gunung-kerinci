package com.dicoding.gunungkerinci.ui.panic_button

import com.dicoding.gunungkerinci.model.DisasterReportItem
import com.dicoding.gunungkerinci.model.SosActiveData
import com.dicoding.gunungkerinci.model.SosCallOption
import com.dicoding.gunungkerinci.model.SosChatMessage

/** State layar panic button / SOS. */
internal data class SosUiState(
    val isSending: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val cooldownSeconds: Int? = null,
    val queuedOffline: Boolean = false,
    val activeSos: SosActiveData? = null,
    val isLoadingActive: Boolean = false,
    val callOptions: List<SosCallOption> = emptyList(),
    val isLoadingCallOptions: Boolean = false
)

/** State layar chat SOS. */
internal data class SosChatUiState(
    val messages: List<SosChatMessage> = emptyList(),
    val queuedMessages: List<QueuedChatUi> = emptyList(),
    val queuedInfo: String? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

/** Pesan chat yang menunggu dikirim (antrean offline) — tampil sebagai bubble pending. */
internal data class QueuedChatUi(
    val localId: String,
    val content: String?,
    val isImage: Boolean,
    val imagePath: String?,
    val timeLabel: String
)

/** State layar laporan bencana. */
internal data class DisasterReportUiState(
    val isSending: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val reports: List<DisasterReportItem> = emptyList(),
    val isLoadingReports: Boolean = false
)
