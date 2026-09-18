package com.dicoding.gunungkerinci.ui.peringatan_dini

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dicoding.gunungkerinci.data.repository.EmergencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel tipis untuk peringatan dini bahaya.
 *
 * Semua keputusan state ada di [EarlyWarningReducer] / [EarlyWarningSelector]
 * (logika murni yang diuji lewat PeringatanDiniTddTest). ViewModel hanya
 * mengurus kapan memanggil API dan menyimpan state untuk UI.
 */
internal class PeringatanDiniViewModel(
    private val emergencyRepository: EmergencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EarlyWarningUiState())
    val uiState: StateFlow<EarlyWarningUiState> = _uiState.asStateFlow()

    /** Peringatan yang terakhir otomatis dibuka popup-nya (cegah popup muncul berulang). */
    private var lastAutoShownWarningId: Int? = null

    /** Ambil peringatan aktif. Dipanggil saat layar tampil dan saat polling. */
    fun load(token: String) {
        viewModelScope.launch {
            emergencyRepository.getActiveWarnings(token)
                .onSuccess { warnings ->
                    val (state, shownId) = EarlyWarningReducer.onNewWarnings(
                        current = _uiState.value,
                        warnings = warnings,
                        lastAutoShownId = lastAutoShownWarningId
                    )
                    lastAutoShownWarningId = shownId
                    _uiState.value = state
                }
                .onFailure { _uiState.value = EarlyWarningReducer.fromError(it.message) }
        }
    }

    /** User menekan tombol X pada popup. */
    fun dismissPopup() {
        _uiState.update { EarlyWarningReducer.dismissPopup(it) }
    }

    /** User menekan bubble untuk membuka popup lagi. */
    fun openPopup() {
        _uiState.update { EarlyWarningReducer.showPopup(it) }
    }
}

internal class PeringatanDiniViewModelFactory(
    private val emergencyRepository: EmergencyRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PeringatanDiniViewModel::class.java)) {
            return PeringatanDiniViewModel(emergencyRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
