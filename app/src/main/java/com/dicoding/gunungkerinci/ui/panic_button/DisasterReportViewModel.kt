package com.dicoding.gunungkerinci.ui.panic_button

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dicoding.gunungkerinci.data.repository.DisasterReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

/** Laporan potensi bencana: kirim + muat riwayat. */
internal class DisasterReportViewModel(
    private val disasterReportRepository: DisasterReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DisasterReportUiState())
    val uiState: StateFlow<DisasterReportUiState> = _uiState.asStateFlow()

    fun submit(
        token: String,
        potensiBencana: String,
        deskripsi: String,
        lokasi: String,
        latitude: Double?,
        longitude: Double?,
        lampiran: MultipartBody.Part?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, successMessage = null, errorMessage = null) }
            disasterReportRepository.submit(
                token = token,
                potensiBencana = potensiBencana,
                deskripsi = deskripsi,
                lokasi = lokasi,
                latitude = latitude,
                longitude = longitude,
                lampiran = lampiran
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(isSending = false, successMessage = "Laporan bencana terkirim.")
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSending = false, errorMessage = error.message ?: "Gagal mengirim laporan")
                    }
                }
        }
    }

    fun loadReports(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReports = true, errorMessage = null) }
            disasterReportRepository.getMyReports(token)
                .onSuccess { list -> _uiState.update { it.copy(isLoadingReports = false, reports = list) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoadingReports = false, errorMessage = error.message ?: "Gagal memuat laporan")
                    }
                }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}

internal class DisasterReportViewModelFactory(
    private val disasterReportRepository: DisasterReportRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DisasterReportViewModel::class.java)) {
            return DisasterReportViewModel(disasterReportRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
