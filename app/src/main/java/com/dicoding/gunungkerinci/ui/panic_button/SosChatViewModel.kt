package com.dicoding.gunungkerinci.ui.panic_button

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dicoding.gunungkerinci.data.local.sos.PendingChatEntity
import com.dicoding.gunungkerinci.data.repository.SosChatRepository
import com.dicoding.gunungkerinci.data.repository.SosRepository
import com.dicoding.gunungkerinci.data.sync.SosQueueRule
import com.dicoding.gunungkerinci.data.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** Chat darurat: cari SOS aktif, muat riwayat, kirim teks atau gambar. */
internal class SosChatViewModel(
    private val sosChatRepository: SosChatRepository,
    private val sosRepository: SosRepository,
    private val syncScheduler: SyncScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosChatUiState())
    val uiState: StateFlow<SosChatUiState> = _uiState.asStateFlow()

    private var sosId: Int? = null
    private var ikutiJob: Job? = null
    private val messagesMutex = Mutex()

    /** Dibatalkan oleh lifecycle layar ketika chat tidak lagi terlihat. */
    suspend fun pollMessages(token: String) {
        start(token)
        while (currentCoroutineContext().isActive) {
            delay(3_000L)
            if (_uiState.value.isLoading || _uiState.value.isSending) continue
            val id = sosId
            if (id == null) start(token) else loadMessages(token, id)
        }
    }

    /** Dipanggil saat layar dibuka: cari SOS aktif lalu muat pesannya. */
    fun start(token: String) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                sosRepository.getActive(token)
                    .onSuccess { active ->
                        if (active == null) {
                            sosId = null
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    messages = emptyList(),
                                    queuedMessages = emptyList(),
                                    isSosActive = false,
                                    errorMessage = "Tidak ada SOS aktif."
                                )
                            }
                        } else {
                            sosId = active.id
                            _uiState.update { it.copy(isSosActive = true) }
                            loadMessages(token, active.id)
                            ikutiAntrean(token, active.id)
                        }
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSosActive = false,
                                errorMessage = pesanJaringan(error, "Gagal memuat status SOS")
                            )
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh(token: String) = start(token)

    fun sendText(token: String, text: String) {
        if (_uiState.value.isSending || text.isBlank()) return
        // Chat hanya bisa dikirim setelah SOS aktif, jadi beri tahu alasannya.
        val id = sosId
        if (id == null) {
            _uiState.update {
                it.copy(errorMessage = "Pesan tidak terkirim. Aktifkan SOS dulu untuk memulai chat darurat.")
            }
            return
        }
        _uiState.update { it.copy(isSending = true, errorMessage = null, queuedInfo = null) }
        viewModelScope.launch {
            try {
                sosChatRepository.sendText(token, id, text)
                    .onSuccess {
                        loadMessages(token, id)
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        antreJikaOffline(error, id, type = "text", content = text, imagePath = null)
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

    fun sendImage(token: String, imageFile: File? = null, caption: String? = null, prepareImage: (suspend () -> File)? = null) {
        if (_uiState.value.isSending) return
        // Sama seperti sendText: butuh SOS aktif.
        val id = sosId
        if (id == null) {
            _uiState.update {
                it.copy(errorMessage = "Pesan tidak terkirim. Aktifkan SOS dulu untuk memulai chat darurat.")
            }
            return
        }
        _uiState.update { it.copy(isSending = true, errorMessage = null, queuedInfo = null) }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isPreparingImage = prepareImage != null) }
                val preparedFile = withContext(Dispatchers.IO) { prepareImage?.invoke() ?: requireNotNull(imageFile) }
                _uiState.update { it.copy(isPreparingImage = false) }
                sosChatRepository.sendImageFile(token, id, preparedFile, caption)
                    .onSuccess {
                        withContext(Dispatchers.IO) { preparedFile.delete() } // lampiran sudah hidup di server
                        loadMessages(token, id)
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        antreJikaOffline(error, id, type = "image", content = caption, imagePath = preparedFile.absolutePath)
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isSending = false, isPreparingImage = false) }
            }
        }
    }

    private suspend fun loadMessages(token: String, id: Int) = messagesMutex.withLock {
        sosChatRepository.getMessages(token, id)
            .onSuccess { data ->
                _uiState.update {
                    it.copy(isLoading = false, messages = data.messages.orEmpty(), errorMessage = null)
                }
            }
            .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = pesanJaringan(error, "Gagal memuat pesan"))
                }
            }
    }

    /** Simpan pesan ke antrean lokal supaya dikirim lagi saat jaringan tersedia. */
    private suspend fun antreJikaOffline(error: Throwable, sosId: Int, type: String, content: String?, imagePath: String?) {
        if (!SosQueueRule.simpanOffline(error)) {
            _uiState.update { it.copy(isSending = false, errorMessage = pesanJaringan(error, "Gagal mengirim pesan")) }
            return
        }
        val tersimpan = sosChatRepository.savePendingChat(sosId, type, content, imagePath)
        if (tersimpan) {
            syncScheduler?.scheduleChatSync()
            _uiState.update { it.copy(isSending = false, queuedInfo = "Tidak ada sinyal, pesan disimpan & akan dikirim otomatis.") }
        } else {
            _uiState.update { it.copy(isSending = false, errorMessage = "Gagal menyimpan pesan") }
        }
    }

    /** Ikuti isi antrean lokal, supaya layar ikut berubah begitu worker selesai mengirim. */
    private fun ikutiAntrean(token: String, sosId: Int) {
        ikutiJob?.cancel()
        ikutiJob = viewModelScope.launch {
            var sebelumnya = 0
            sosChatRepository.observePendingChats(sosId).collect { antrean ->
                _uiState.update { it.copy(queuedMessages = antrean.map { entity -> entity.toQueuedUi() }) }
                if (antrean.size < sebelumnya) {
                    // Ada pesan yang terkirim: buang notifikasi lama lalu muat riwayat terbaru.
                    _uiState.update { it.copy(queuedInfo = null) }
                    loadMessages(token, sosId)
                }
                sebelumnya = antrean.size
            }
        }
    }

    private fun PendingChatEntity.toQueuedUi(): QueuedChatUi = QueuedChatUi(
        localId = localId,
        content = content,
        isImage = type == "image",
        imagePath = imagePath,
        timeLabel = com.dicoding.gunungkerinci.util.DateTimeFormat.epochToWibHm(createdAt).orEmpty()
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
