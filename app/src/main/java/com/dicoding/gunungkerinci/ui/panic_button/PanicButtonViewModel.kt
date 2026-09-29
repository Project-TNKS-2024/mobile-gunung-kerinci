package com.dicoding.gunungkerinci.ui.panic_button

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dicoding.gunungkerinci.data.repository.SosCallRepository
import com.dicoding.gunungkerinci.data.repository.SosCooldownException
import com.dicoding.gunungkerinci.data.repository.SosRepository
import com.dicoding.gunungkerinci.data.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Panic Button: kirim SOS + pantau status SOS aktif.
 * Logika validasi ada di [SosTriggerValidator]; keputusan offline di [SosQueueRule].
 */
internal class PanicButtonViewModel(
    private val sosRepository: SosRepository,
    private val syncScheduler: SyncScheduler? = null,
    private val sosCallRepository: SosCallRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    fun sendSos(
        token: String,
        latitude: Double,
        longitude: Double,
        severity: String,
        message: String?
    ) {
        // TDD C01–C04 di runtime: tolak input tidak valid sebelum menyentuh jaringan.
        // Panic murni boleh tanpa pesan (wajibPesan = false, backend `message` opsional).
        val validasiError = SosTriggerValidator.error(latitude, longitude, severity, message)
        if (validasiError != null) {
            _uiState.update { it.copy(isSending = false, errorMessage = validasiError) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    successMessage = null,
                    errorMessage = null,
                    cooldownSeconds = null,
                    queuedOffline = false
                )
            }

            sosRepository.trigger(token, latitude, longitude, severity, message)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            successMessage = "SOS terkirim. Bantuan sedang diproses."
                        )
                    }
                }
                .onFailure { error ->
                    when {
                        error is SosCooldownException -> _uiState.update {
                            it.copy(
                                isSending = false,
                                cooldownSeconds = error.waitSeconds,
                                errorMessage = error.message
                            )
                        }
                        // Tidak ada sinyal → simpan ke antrean lokal lalu jadwalkan sync.
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
                                    queuedOffline = tersimpan,
                                    successMessage = if (tersimpan) {
                                        "Tidak ada sinyal — SOS disimpan & akan dikirim otomatis."
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
        }
    }

    /** Muat status SOS aktif (untuk banner). Gagal → diamkan (non-blocking). */
    fun loadActive(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingActive = true) }
            sosRepository.getActive(token)
                .onSuccess { data -> _uiState.update { it.copy(isLoadingActive = false, activeSos = data) } }
                .onFailure { _uiState.update { it.copy(isLoadingActive = false) } }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(successMessage = null, errorMessage = null, cooldownSeconds = null, queuedOffline = false)
        }
    }

    /** Muat kontak darurat (tel/WA) dari backend untuk tombol panggilan. */
    fun loadCallOptions(token: String) {
        val repo = sosCallRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCallOptions = true) }
            repo.getCallOptions(token)
                .onSuccess { list -> _uiState.update { it.copy(isLoadingCallOptions = false, callOptions = list) } }
                .onFailure { _uiState.update { it.copy(isLoadingCallOptions = false) } }
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
