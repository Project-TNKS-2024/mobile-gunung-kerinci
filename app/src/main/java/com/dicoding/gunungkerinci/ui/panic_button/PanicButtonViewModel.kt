package com.dicoding.gunungkerinci.ui.panic_button

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dicoding.gunungkerinci.data.repository.SosCallRepository
import com.dicoding.gunungkerinci.data.repository.SosCooldownException
import com.dicoding.gunungkerinci.data.repository.SosRepository
import com.dicoding.gunungkerinci.data.sync.SosQueueRule
import com.dicoding.gunungkerinci.data.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Kirim SOS dan pantau status SOS yang aktif. */
internal class PanicButtonViewModel(
    private val sosRepository: SosRepository,
    private val syncScheduler: SyncScheduler? = null,
    private val sosCallRepository: SosCallRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private var currentToken: String = ""

    init {
        viewModelScope.launch {
            sosRepository.observeLatestPending().collect { pending ->
                val queued = pending?.syncStatus in setOf("PENDING", "FAILED", "SYNCING")
                val wasQueued = _uiState.value.queuedOffline
                _uiState.update {
                    it.copy(queuedOffline = queued,
                        errorMessage = if (wasQueued && pending?.syncStatus == "REJECTED")
                            "SOS dalam antrean ditolak server. Periksa status dan coba lagi." else it.errorMessage)
                }
                if (wasQueued && !queued && currentToken.isNotBlank()) loadActive(currentToken)
            }
        }
    }

    fun sendSos(
        token: String,
        latitude: Double,
        longitude: Double,
        severity: String,
        message: String?
    ) {
        if (_uiState.value.isSending || _uiState.value.isPreparingLocation) return
        currentToken = token
        // Tolak input tidak valid sebelum memanggil jaringan.
        val validasiError = SosTriggerValidator.error(latitude, longitude, severity, message)
        if (validasiError != null) {
            _uiState.update { it.copy(isSending = false, errorMessage = validasiError) }
            return
        }

        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isSending = true,
                        successMessage = null,
                        errorMessage = null,
                        cooldownSeconds = null
                    )
                }

                sosRepository.trigger(token, latitude, longitude, severity, message)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                successMessage = "SOS terkirim ke server."
                            )
                        }
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        when {
                            error is SosCooldownException -> _uiState.update {
                                it.copy(
                                    isSending = false,
                                    cooldownSeconds = error.waitSeconds,
                                    errorMessage = error.message
                                )
                            }
                            // Tidak ada sinyal, simpan ke antrean lokal lalu jadwalkan sync.
                            SosQueueRule.simpanOffline(error) -> {
                                val tersimpan = sosRepository.savePendingSos(
                                    latitude = latitude,
                                    longitude = longitude,
                                    severity = severity,
                                    message = message
                                )
                                if (tersimpan) syncScheduler?.scheduleSosSync()
                                _uiState.update {
                                    it.copy(
                                        isSending = false,
                                        queuedOffline = tersimpan || it.queuedOffline,
                                        successMessage = if (tersimpan) {
                                            "SOS tersimpan di perangkat, menunggu koneksi."
                                        } else null,
                                        errorMessage = if (tersimpan) null
                                        else (error.message ?: "Gagal menyimpan SOS")
                                    )
                                }
                            }
                            else -> _uiState.update {
                                it.copy(isSending = false, errorMessage = error.message ?: "Gagal mengirim SOS")
                            }
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun prepareAndSendSos(token: String, severity: String, location: suspend () -> Pair<Double, Double>?) {
        if (_uiState.value.isSending || _uiState.value.isPreparingLocation) return
        _uiState.update { it.copy(isPreparingLocation = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            try {
                val point = location()
                if (point == null) {
                    _uiState.update { it.copy(errorMessage = "Lokasi belum tersedia. Nyalakan GPS lalu coba lagi.") }
                } else {
                    _uiState.update { it.copy(isPreparingLocation = false) }
                    sendSos(token, point.first, point.second, severity, null)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Gagal mencari lokasi") }
            } finally {
                _uiState.update { it.copy(isPreparingLocation = false) }
            }
        }
    }

    /** Muat status SOS aktif untuk banner. */
    fun loadActive(token: String) {
        currentToken = token
        if (_uiState.value.isLoadingActive) return
        _uiState.update { it.copy(isLoadingActive = true, activeError = null) }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingActive = true) }
                sosRepository.getActive(token)
                    .onSuccess { data -> _uiState.update { it.copy(isLoadingActive = false, activeSos = data) } }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _uiState.update { it.copy(isLoadingActive = false, activeError = error.message ?: "Gagal memuat status SOS") }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(activeError = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isLoadingActive = false) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(successMessage = null, errorMessage = null, cooldownSeconds = null)
        }
    }

    /** Muat kontak darurat untuk tombol panggilan. */
    fun loadCallOptions(token: String) {
        val repo = sosCallRepository ?: return
        if (_uiState.value.isLoadingCallOptions) return
        _uiState.update { it.copy(isLoadingCallOptions = true, callOptionsError = null) }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingCallOptions = true) }
                repo.getCallOptions(token)
                    .onSuccess { list -> _uiState.update { it.copy(isLoadingCallOptions = false, callOptions = list) } }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _uiState.update { it.copy(isLoadingCallOptions = false, callOptionsError = error.message ?: "Gagal memuat kontak petugas") }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(callOptionsError = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isLoadingCallOptions = false) }
            }
        }
    }
}

internal class PanicButtonViewModelFactory(
    private val sosRepository: SosRepository,
    private val syncScheduler: SyncScheduler? = null,
    private val sosCallRepository: SosCallRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PanicButtonViewModel::class.java)) {
            return PanicButtonViewModel(sosRepository, syncScheduler, sosCallRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
