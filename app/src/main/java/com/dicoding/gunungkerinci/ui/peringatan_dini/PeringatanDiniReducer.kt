package com.dicoding.gunungkerinci.ui.peringatan_dini

/**
 * Perubahan state layar peringatan dini.
 * Popup muncul hanya bila ada peringatan aktif; menutup popup TIDAK menghapus datanya.
 */
internal object PeringatanDiniReducer {

    fun fromWarnings(warnings: List<PeringatanDini>): PeringatanDiniUiState {
        val active = PeringatanDiniSelector.select(warnings)
        return PeringatanDiniUiState(activeWarning = active, popupVisible = active != null)
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
        current: PeringatanDiniUiState,
        warnings: List<PeringatanDini>,
        lastAutoShownId: Int?
    ): Pair<PeringatanDiniUiState, Int?> {
        val active = PeringatanDiniSelector.select(warnings)
        if (active == null) {
            // Tidak ada peringatan aktif → bubble & popup hilang.
            // Reset id (A20): bila peringatan ber-ID sama muncul lagi nanti,
            // popup-nya boleh muncul otomatis lagi.
            return PeringatanDiniUiState() to null
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
    fun showPopup(state: PeringatanDiniUiState): PeringatanDiniUiState =
        if (state.activeWarning != null) state.copy(popupVisible = true) else state

    /**
     * Gagal ambil data → state aman, aplikasi tidak error.
     *
     * [isTerminal] = true untuk gagal permanen (401 sesi habis / 403 tidak ada
     * pendakian aktif): peringatan lama DIHAPUS karena sudah tidak berlaku.
     * [isTerminal] = false untuk gagal sementara (jaringan/5xx): peringatan lama
     * TETAP ditampilkan — blip jaringan tidak boleh menyembunyikan bahaya asli.
     */
    fun fromError(
        current: PeringatanDiniUiState,
        message: String?,
        isTerminal: Boolean
    ): PeringatanDiniUiState =
        if (isTerminal) PeringatanDiniUiState(errorMessage = message)
        else current.copy(errorMessage = message)

    fun dismissPopup(state: PeringatanDiniUiState): PeringatanDiniUiState =
        state.copy(popupVisible = false)
}
