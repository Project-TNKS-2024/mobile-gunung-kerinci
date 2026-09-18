package com.dicoding.gunungkerinci.ui.peringatan_dini

/**
 * Perubahan state layar peringatan dini.
 * Popup muncul hanya bila ada peringatan aktif; menutup popup TIDAK menghapus datanya.
 */
internal object EarlyWarningReducer {

    fun fromWarnings(warnings: List<EmergencyWarning>): EarlyWarningUiState {
        val active = EarlyWarningSelector.select(warnings)
        return EarlyWarningUiState(activeWarning = active, popupVisible = active != null)
    }

    /**
     * Dipakai saat polling.
     *
     * Popup otomatis terbuka HANYA untuk peringatan baru. Kalau peringatan masih sama
     * dan user sudah menutup popup-nya, popup tetap tertutup (tidak muncul ulang).
     *
     * @return state baru + id peringatan yang terakhir otomatis dibuka.
     */
    fun onNewWarnings(
        current: EarlyWarningUiState,
        warnings: List<EmergencyWarning>,
        lastAutoShownId: Int?
    ): Pair<EarlyWarningUiState, Int?> {
        val active = EarlyWarningSelector.select(warnings)
        if (active == null) {
            // Tidak ada peringatan aktif → bubble & popup hilang.
            return EarlyWarningUiState() to lastAutoShownId
        }

        val isNew = active.id != lastAutoShownId
        val next = current.copy(
            activeWarning = active,
            errorMessage = null,
            popupVisible = if (isNew) true else current.popupVisible
        )
        return next to (if (isNew) active.id else lastAutoShownId)
    }

    /** Bubble diklik → buka popup lagi (hanya bila ada peringatan aktif). */
    fun showPopup(state: EarlyWarningUiState): EarlyWarningUiState =
        if (state.activeWarning != null) state.copy(popupVisible = true) else state

    /** Gagal ambil data (mis. jaringan) → state aman, aplikasi tidak error. */
    fun fromError(message: String?): EarlyWarningUiState =
        EarlyWarningUiState(activeWarning = null, popupVisible = false, errorMessage = message)

    fun dismissPopup(state: EarlyWarningUiState): EarlyWarningUiState =
        state.copy(popupVisible = false)
}
