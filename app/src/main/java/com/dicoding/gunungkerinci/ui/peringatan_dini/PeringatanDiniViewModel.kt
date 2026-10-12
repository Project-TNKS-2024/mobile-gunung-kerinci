package com.dicoding.gunungkerinci.ui.peringatan_dini

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dicoding.gunungkerinci.data.repository.PeringatanDiniApiException
import com.dicoding.gunungkerinci.data.repository.PeringatanDiniRepository
import com.dicoding.gunungkerinci.data.repository.TrackingGpsRepository
import com.dicoding.gunungkerinci.util.DateTimeFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PeringatanDiniViewModel(
    private val peringatanDiniRepository: PeringatanDiniRepository,
    private val trackingGpsRepository: TrackingGpsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeringatanDiniUiState())
    val uiState: StateFlow<PeringatanDiniUiState> = _uiState.asStateFlow()

    // id peringatan yang terakhir dibuka otomatis, biar popup tidak muncul berulang
    private var lastAutoShownWarningId: Int? = null

    // Ambil peringatan aktif. Dipanggil saat layar tampil dan saat polling.
    // ownerKey = pemilik cache; "" berarti cache tidak dipakai.
    fun load(token: String, ownerKey: String = "") {
        viewModelScope.launch {
            // Tampilkan dulu dari cache, biar tidak senyap saat dibuka tanpa jaringan.
            if (_uiState.value.activeWarning == null) {
                val cached = peringatanDiniRepository.cachedFor(ownerKey)
                if (cached.isNotEmpty()) {
                    val at = DateTimeFormat.epochToWibHm(peringatanDiniRepository.cachedAt(ownerKey))
                    _uiState.value = PeringatanDiniReducer.fromCache(cached, at)
                }
            }

            peringatanDiniRepository.getActiveWarnings(token)
                .onSuccess { warnings ->
                    // Simpan hasil terbaru ke cache milik user sekarang.
                    peringatanDiniRepository.save(ownerKey, warnings, System.currentTimeMillis())

                    val (state, shownId) = PeringatanDiniReducer.onNewWarnings(
                        current = _uiState.value,
                        warnings = warnings,
                        lastAutoShownId = lastAutoShownWarningId
                    )
                    lastAutoShownWarningId = shownId
                    _uiState.value = state.copy(
                        lastUpdatedAt = DateTimeFormat.nowWibHm(),
                        isStale = false
                    )

                    if (state.activeWarning != null) {
                        fetchLastPosition(token)
                    }
                }
                .onFailure { e ->
                    // 401/403 = gagal permanen, peringatan lama dihapus.
                    // Gagal lain (jaringan/5xx) cuma sementara, tetap tampil.
                    val terminal = (e as? PeringatanDiniApiException)?.code in listOf(401, 403)
                    val errorState = PeringatanDiniReducer.fromError(_uiState.value, e.message, terminal)
                    _uiState.value = errorState.copy(
                        // Data cache yang gagal disegarkan ditandai usang.
                        isStale = !terminal && errorState.activeWarning != null
                    )
                }
        }
    }

    // Pembaruan lokasi manual dari tombol di popup peringatan
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
                        it.copy(isUpdatingLocation = false, lastLocationUpdate = DateTimeFormat.nowWibHm())
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isUpdatingLocation = false, locationUpdateMessage = error.message ?: "Gagal memperbarui lokasi")
                    }
                }
        }
    }

    // Posisi terakhir dari server (gk_tracking)
    private fun fetchLastPosition(token: String) {
        val repo = trackingGpsRepository ?: return
        viewModelScope.launch {
            repo.getMyPosition(token)
                .onSuccess { iso ->
                    DateTimeFormat.isoToWibHm(iso)?.let { label ->
                        _uiState.update { it.copy(lastLocationUpdate = label) }
                    }
                }
                .onFailure {  }
        }
    }

    // Tombol "Update Lokasi": tandai sedang mencari sinyal GPS.
    fun mulaiCariLokasi() {
        _uiState.update { it.copy(isUpdatingLocation = true, locationUpdateMessage = null) }
    }

    // Pencarian lokasi gagal, tampilkan alasannya di popup
    fun gagalCariLokasi(pesan: String) {
        _uiState.update { it.copy(isUpdatingLocation = false, locationUpdateMessage = pesan) }
    }

    // Tutup popup peringatan dini
    fun dismissPopup() {
        _uiState.update { PeringatanDiniReducer.dismissPopup(it) }
    }

    // Buka popup peringatan dini
    fun openPopup() {
        _uiState.update { PeringatanDiniReducer.showPopup(it) } 
    }
}

internal class PeringatanDiniViewModelFactory(
    private val peringatanDiniRepository: PeringatanDiniRepository,
    private val trackingGpsRepository: TrackingGpsRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PeringatanDiniViewModel::class.java)) {
            return PeringatanDiniViewModel(peringatanDiniRepository, trackingGpsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
