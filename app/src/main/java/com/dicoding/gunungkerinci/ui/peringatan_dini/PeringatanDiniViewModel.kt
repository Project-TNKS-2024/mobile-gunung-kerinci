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
    private var openWhenLoaded = false

    // Ambil peringatan aktif. Dipanggil saat layar tampil dan saat polling.
    // ownerKey = pemilik cache; "" berarti cache tidak dipakai.
    fun load(token: String, ownerKey: String = "") {
        if (_uiState.value.isLoadingWarnings) return
        _uiState.update { it.copy(isLoadingWarnings = true) }
        viewModelScope.launch {
            try {
                // Tampilkan dulu dari cache, biar tidak senyap saat dibuka tanpa jaringan.
                if (_uiState.value.activeWarning == null) {
                    val cached = peringatanDiniRepository.cachedFor(ownerKey)
                    if (cached.isNotEmpty()) {
                        val at = DateTimeFormat.epochToWibHm(peringatanDiniRepository.cachedAt(ownerKey))
                        _uiState.update { current -> current.copy(activeWarning = PeringatanDiniReducer.fromCache(cached, at).activeWarning, lastUpdatedAt = at, isStale = true) }
                        if (openWhenLoaded) {
                            _uiState.update { PeringatanDiniReducer.showPopup(it) }
                            openWhenLoaded = false
                        }
                    }
                }

                peringatanDiniRepository.getActiveWarnings(token)
                    .onSuccess { warnings ->
                        // The screen may succeed while the service's independent poll fails.
                        // Let the service apply the same session, permission and receipt checks.
                        WarningMonitoring.reportFetchedWarnings(token, warnings)
                        // Simpan hasil terbaru ke cache milik user sekarang.
                        peringatanDiniRepository.save(ownerKey, warnings, System.currentTimeMillis())

                        val (state, shownId) = PeringatanDiniReducer.onNewWarnings(
                            current = _uiState.value,
                            warnings = warnings,
                            lastAutoShownId = lastAutoShownWarningId
                        )
                        lastAutoShownWarningId = shownId
                        _uiState.value = state.copy(
                            popupVisible = state.popupVisible || openWhenLoaded,
                            lastUpdatedAt = DateTimeFormat.nowWibHm(),
                            isStale = false,
                            hasLoadedWarnings = true
                        )
                        openWhenLoaded = false

                        if (state.activeWarning != null) {
                            fetchLastPosition(token)
                        }
                    }
                    .onFailure { e ->
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        // 401/403 = gagal permanen, peringatan lama dihapus.
                        // Gagal lain (jaringan/5xx) cuma sementara, tetap tampil.
                        val terminal = (e as? PeringatanDiniApiException)?.code in listOf(401, 403)
                        val errorState = PeringatanDiniReducer.fromError(_uiState.value, e.message, terminal)
                        _uiState.value = errorState.copy(
                            // Data cache yang gagal disegarkan ditandai usang.
                            isStale = !terminal && errorState.activeWarning != null
                        )
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isLoadingWarnings = false) }
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
            _uiState.update { it.copy(isUpdatingLocation = false, isSendingLocation = false, locationUpdateMessage = "Layanan lokasi tidak tersedia") }
            return
        }
        if (_uiState.value.isSendingLocation) return
        _uiState.update { it.copy(isUpdatingLocation = true, isSendingLocation = true, locationUpdateMessage = null) }
        viewModelScope.launch {
            try {
                repo.kirimTitik(token, latitude, longitude, altitude, accuracy, null)
                    .onSuccess {
                        _uiState.update {
                            it.copy(isUpdatingLocation = false, lastLocationUpdate = DateTimeFormat.nowWibHm())
                        }
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _uiState.update {
                            it.copy(isUpdatingLocation = false, locationUpdateMessage = error.message ?: "Gagal memperbarui lokasi")
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(locationUpdateMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isUpdatingLocation = false, isSendingLocation = false) }
            }
        }
    }

    // Posisi terakhir dari server (gk_tracking)
    private fun fetchLastPosition(token: String) {
        val repo = trackingGpsRepository ?: return
        viewModelScope.launch {
            repo.getMyPosition(token)
                .onSuccess { iso ->
                    DateTimeFormat.apiWibHm(iso)?.let { label ->
                        _uiState.update { it.copy(lastLocationUpdate = label) }
                    }
                }
                .onFailure {  }
        }
    }

    // Tombol "Update Lokasi": tandai sedang mencari sinyal GPS.
    fun mulaiCariLokasi(): Boolean {
        if (_uiState.value.isUpdatingLocation) return false
        _uiState.update { it.copy(isUpdatingLocation = true, isSendingLocation = false, locationUpdateMessage = null) }
        return true
    }

    // Pencarian lokasi gagal, tampilkan alasannya di popup
    fun gagalCariLokasi(pesan: String) {
        _uiState.update { it.copy(isUpdatingLocation = false, isSendingLocation = false, locationUpdateMessage = pesan) }
    }

    // Tutup popup peringatan dini
    fun dismissPopup() {
        openWhenLoaded = false
        _uiState.update { PeringatanDiniReducer.dismissPopup(it) }
    }

    // Buka popup peringatan dini
    fun openNotificationWarning(warning: PeringatanDini?) {
        if (warning != null) {
            _uiState.update { it.copy(activeWarning = warning, popupVisible = true) }
            openWhenLoaded = false
        } else openPopup()
    }

    fun openPopup() {
        openWhenLoaded = _uiState.value.activeWarning == null
        _uiState.update { it.copy(popupVisible = true) }
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
