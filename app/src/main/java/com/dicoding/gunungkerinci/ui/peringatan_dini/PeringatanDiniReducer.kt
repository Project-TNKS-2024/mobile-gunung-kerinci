package com.dicoding.gunungkerinci.ui.peringatan_dini

internal object PeringatanDiniReducer {

    fun fromWarnings(warnings: List<PeringatanDini>): PeringatanDiniUiState {
        val active = PeringatanDiniSelector.select(warnings)
        return PeringatanDiniUiState(activeWarning = active, popupVisible = active != null)
    }

    // Tampilkan peringatan dari cache. Popup sengaja tidak dibuka otomatis, cukup
    // bubble-nya, biar tidak muncul terus tiap app dibuka.
    fun fromCache(items: List<PeringatanDini>, lastUpdatedAt: String?): PeringatanDiniUiState {
        val active = PeringatanDiniSelector.select(items) ?: return PeringatanDiniUiState()
        return PeringatanDiniUiState(
            activeWarning = active,
            popupVisible = false,
            lastUpdatedAt = lastUpdatedAt,
            isStale = true
        )
    }

    // Dipakai saat polling. Popup cuma dibuka kalau ada peringatan baru; kalau masih
    // yang sama dan sudah ditutup user, biarkan tertutup.
    fun onNewWarnings(
        current: PeringatanDiniUiState,
        warnings: List<PeringatanDini>,
        lastAutoShownId: Int?
    ): Pair<PeringatanDiniUiState, Int?> {
        val active = PeringatanDiniSelector.select(warnings)
        if (active == null) {
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

    // Bubble diklik, buka popup lagi kalau ada peringatan aktif.
    fun showPopup(state: PeringatanDiniUiState): PeringatanDiniUiState =
        if (state.activeWarning != null) state.copy(popupVisible = true) else state

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
