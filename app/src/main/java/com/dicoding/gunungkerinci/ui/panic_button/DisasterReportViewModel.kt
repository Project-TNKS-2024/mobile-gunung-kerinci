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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody

/** Laporan bencana: kirim laporan dan muat riwayatnya. */
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
        lampiran: MultipartBody.Part? = null,
        prepareAttachment: (suspend () -> MultipartBody.Part?)? = null
    ) {
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(isSending = true, isPreparingAttachment = prepareAttachment != null, successMessage = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                val attachment = withContext(Dispatchers.IO) { prepareAttachment?.invoke() ?: lampiran }
                _uiState.update { it.copy(isPreparingAttachment = false) }
                _uiState.update { it.copy(isSending = true, successMessage = null, errorMessage = null) }
                disasterReportRepository.submit(
                    token = token,
                    potensiBencana = potensiBencana,
                    deskripsi = deskripsi,
                    lokasi = lokasi,
                    latitude = latitude,
                    longitude = longitude,
                    lampiran = attachment
                )
                    .onSuccess {
                        _uiState.update {
                            it.copy(isSending = false, successMessage = "Laporan bencana terkirim.")
                        }
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _uiState.update {
                            it.copy(isSending = false, errorMessage = pesanJaringan(error, "Gagal mengirim laporan"))
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isSending = false, isPreparingAttachment = false) }
            }
        }
    }

    fun loadReports(token: String) {
        if (_uiState.value.isLoadingReports) return
        _uiState.update { it.copy(isLoadingReports = true) }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingReports = true, errorMessage = null) }
                disasterReportRepository.getMyReports(token)
                    .onSuccess { list -> _uiState.update { it.copy(isLoadingReports = false, reports = list) } }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _uiState.update {
                            it.copy(isLoadingReports = false, errorMessage = pesanJaringan(error, "Gagal memuat laporan"))
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isLoadingReports = false) }
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
