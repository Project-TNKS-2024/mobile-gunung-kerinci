package com.dicoding.gunungkerinci.ui.panic_button

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dicoding.gunungkerinci.data.local.sos.PendingChatEntity
import com.dicoding.gunungkerinci.data.repository.SosChatRepository
import com.dicoding.gunungkerinci.data.repository.SosRepository
import com.dicoding.gunungkerinci.data.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat SOS: cari SOS aktif → muat riwayat → kirim teks/gambar.
 */
internal class SosChatViewModel(
    private val sosChatRepository: SosChatRepository,
    private val sosRepository: SosRepository,
    private val syncScheduler: SyncScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosChatUiState())
    val uiState: StateFlow<SosChatUiState> = _uiState.asStateFlow()

    private var sosId: Int? = null

    /** Titik masuk layar: cari SOS aktif lalu muat pesannya. */
    fun start(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            sosRepository.getActive(token)
                .onSuccess { active ->
                    if (active == null) {
                        sosId = null
                        _uiState.update {
                            it.copy(isLoading = false, messages = emptyList(), queuedMessages = emptyList(), errorMessage = "Tidak ada SOS aktif.")
                        }
                    } else {
                        sosId = active.id
                        loadMessages(token, active.id)
                        muatAntreanLokal(active.id)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Gagal memuat status SOS")
                    }
                }
        }
    }

    fun refresh(token: String) = start(token)

    fun sendText(token: String, text: String) {
        val id = sosId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null, queuedInfo = null) }
            sosChatRepository.sendText(token, id, text)
                .onSuccess {
                    loadMessages(token, id)
                }
                .onFailure { error ->
                    antreJikaOffline(error, id, type = "text", content = text, imagePath = null)
                }
        }
    }

    fun sendImage(token: String, imageFile: File, caption: String? = null) {
        val id = sosId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null, queuedInfo = null) }
            sosChatRepository.sendImageFile(token, id, imageFile, caption)
                .onSuccess {
                    _uiState.update { it.copy(isSending = false) }
                    runCatching { imageFile.delete() } // lampiran sudah hidup di server
                    loadMessages(token, id)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSending = false) }
                    antreJikaOffline(error, id, type = "image", content = caption, imagePath = imageFile.absolutePath)
                }
        }
    }

    private suspend fun loadMessages(token: String, id: Int) {
        sosChatRepository.getMessages(token, id)
            .onSuccess { data ->
                _uiState.update {
                    it.copy(isSending = false, isLoading = false, messages = data.messages.orEmpty(), errorMessage = null)
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(isSending = false, isLoading = false, errorMessage = error.message ?: "Gagal memuat pesan")
                }
            }
    }

    /**
     * Jalur offline US-12: pesan chat yang gagal karena sinyal disimpan ke Room
     * (`pending_sos_chats`) lalu dikirim ulang otomatis oleh PendingChatSyncWorker.
     * Hanya untuk SOS yang sudah aktif di server — sosId selalu valid.
     */
    private suspend fun antreJikaOffline(error: Throwable, sosId: Int, type: String, content: String?, imagePath: String?) {
        if (!SosQueueRule.simpanOffline(error)) {
            _uiState.update { it.copy(isSending = false, errorMessage = error.message ?: "Gagal mengirim pesan") }
            return
        }
        val tersimpan = sosChatRepository.savePendingChat(sosId, type, content, imagePath)
        if (tersimpan) {
            syncScheduler?.scheduleChatSync()
            muatAntreanLokal(sosId)
            _uiState.update { it.copy(isSending = false, queuedInfo = "Tidak ada sinyal — pesan disimpan & akan dikirim otomatis.") }
        } else {
            _uiState.update { it.copy(isSending = false, errorMessage = "Gagal menyimpan pesan") }
        }
    }

    private suspend fun muatAntreanLokal(sosId: Int) {
        val antrean = sosChatRepository.getPendingChats(sosId)
        _uiState.update { it.copy(queuedMessages = antrean.map { entity -> entity.toQueuedUi() }) }
    }

    private fun PendingChatEntity.toQueuedUi(): QueuedChatUi = QueuedChatUi(
        localId = localId,
        content = content,
        isImage = type == "image",
        imagePath = imagePath,
        timeLabel = SimpleDateFormat("HH.mm", Locale.US).format(Date(createdAt))
    )
}

internal class SosChatViewModelFactory(
    private val sosChatRepository: SosChatRepository,
    private val sosRepository: SosRepository,
    private val syncScheduler: SyncScheduler? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SosChatViewModel::class.java)) {
            return SosChatViewModel(sosChatRepository, sosRepository, syncScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
