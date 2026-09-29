package com.dicoding.gunungkerinci.ui.peringatan_dini

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dicoding.gunungkerinci.data.repository.EmergencyRepository
import com.dicoding.gunungkerinci.data.repository.TrackingGpsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * ViewModel tipis untuk peringatan dini bahaya.
 *
 * Semua keputusan state ada di [EarlyWarningReducer] / [EarlyWarningSelector]
 * (logika murni yang diuji lewat PeringatanDiniTddTest). ViewModel hanya
 * mengurus kapan memanggil API dan menyimpan state untuk UI.
 */
internal class PeringatanDiniViewModel(
    private val emergencyRepository: EmergencyRepository,
    private val trackingGpsRepository: TrackingGpsRepository? = null
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
                    // Caption "Terakhir di update" terisi sejak popup pertama kali
                    // dibuka — diambil dari server (gk_tracking), mencakup semua
                    // jalur kirim lokasi (tombol, otomatis, batch offline).
                    if (state.activeWarning != null) {
                        fetchLastPosition(token)
                    }
                }
                .onFailure { _uiState.value = EarlyWarningReducer.fromError(it.message) }
        }
    }

    /**
     * Jalur manual pembaruan lokasi (tombol pada popup peringatan, sesuai desain):
     * kirim titik posisi terakhir pendaki ke gk_tracking → caption
     * "Terakhir di update HH.mm" diperbarui saat berhasil.
     */
    fun updateLastLocation(
        token: String,
        latitude: Double,
        longitude: Double,
        altitude: Double? = null,
        accuracy: Double? = null
    ) {
        val repo = trackingGpsRepository ?: run {
            _uiState.update { it.copy(locationUpdateMessage = "Layanan lokasi tidak tersedia") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingLocation = true, locationUpdateMessage = null) }
            repo.kirimTitik(token, latitude, longitude, altitude, accuracy, null)
                .onSuccess {
                    _uiState.update {
                        it.copy(isUpdatingLocation = false, lastLocationUpdate = jamSekarang())
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isUpdatingLocation = false, locationUpdateMessage = error.message ?: "Gagal memperbarui lokasi")
                    }
                }
        }
    }

    /** Waktu lokal (WIB) dengan format desimal sesuai desain UI: "14.33". */
    private fun jamSekarang(): String {
        val waktu = SimpleDateFormat("HH.mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        return waktu.format(Date())
    }

    /** Ambil posisi terakhir dari server (gk_tracking) → caption popup peringatan. */
    private fun fetchLastPosition(token: String) {
        val repo = trackingGpsRepository ?: return
        viewModelScope.launch {
            repo.getMyPosition(token)
                .onSuccess { iso ->
                    formatWib(iso)?.let { label ->
                        _uiState.update { it.copy(lastLocationUpdate = label) }
                    }
                }
                .onFailure { /* diamkan — caption bersifat pelengkap */ }
        }
    }

    /** ISO-8601 UTC dari backend → "HH.mm" WIB. Gagal parse → null. */
    private fun formatWib(iso: String?): String? {
        if (iso.isNullOrBlank()) return null
        val bersih = iso.trim().substringBefore('.').removeSuffix("Z")
        val waktu = runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(bersih)
        }.getOrNull() ?: return null
        return SimpleDateFormat("HH.mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.format(waktu)
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
    private val emergencyRepository: EmergencyRepository,
    private val trackingGpsRepository: TrackingGpsRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PeringatanDiniViewModel::class.java)) {
            return PeringatanDiniViewModel(emergencyRepository, trackingGpsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
