package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.repository.ActiveHikeRepository
import com.dicoding.gunungkerinci.data.repository.CheckpointQrRepository
import com.dicoding.gunungkerinci.data.repository.GpsCheckInRepository
import com.dicoding.gunungkerinci.data.repository.ManualCheckInRepository
import com.dicoding.gunungkerinci.data.repository.TrackingGpsRepository
import com.dicoding.gunungkerinci.data.repository.TrackingProgressRepository
import com.dicoding.gunungkerinci.data.repository.TrackingRepository
import com.dicoding.gunungkerinci.data.sync.SyncScheduler
import com.dicoding.gunungkerinci.model.GpsNearestPost
import com.dicoding.gunungkerinci.model.TrackingPost
import com.dicoding.gunungkerinci.model.TrackingProgressHiker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PelacakanJejakUiState(
    // Kartu tiket — sumber: GET /mytiket (lihat TicketUiMapper)
    val isLoadingTickets: Boolean = false,
    val activeTicket: JejakTicketUi? = null,
    val finishedTickets: List<JejakTicketUi> = emptyList(),
    val days: List<JejakDayUi> = emptyList(),
    val periodeLabel: String? = null,
    val isLoadingPosts: Boolean = false,
    val postCheckpoints: List<JejakCheckpointUi> = emptyList(),
    val postErrorMessage: String? = null,
    val noActiveHikeMessage: String? = null,
    val isLoadingProgress: Boolean = false,
    val progressText: String? = null,
    val progressErrorMessage: String? = null,
    val isCheckingIn: Boolean = false,
    val qrScanMessage: String? = null,
    val qrScanErrorMessage: String? = null,
    // GPS check state
    val isCheckingGps: Boolean = false,
    val nearbyPost: GpsNearestPost? = null,         // pos terdekat saat within_radius true
    // Id pos terdekat dari deteksi GPS terakhir (auto maupun manual). Dipakai untuk
    // memastikan QR yang di-scan memang milik pos yang sedang didatangi pendaki.
    val posTerdekatId: Int? = null,
    val gpsCheckErrorMessage: String? = null,
    // Gerbang check-in GPS: Unknown / GpsOff / LuarRadius / DalamRadius
    val gpsGateState: GpsGateState = GpsGateState.Unknown,
    // Manual check-in state
    val isManualCheckingIn: Boolean = false,
    val manualCheckInMessage: String? = null,
    val manualCheckInErrorMessage: String? = null,
    // Data progress mentah — dipakai untuk daftar anggota (section "Cek Anggota")
    val progressHikers: List<TrackingProgressHiker> = emptyList(),
    // Daftar pos jalur (untuk hitung jarak antar-pos di layar Location Detail)
    val loadedPosts: List<TrackingPost> = emptyList()
)

internal class PelacakanJejakViewModel(
    private val activeHikeRepository: ActiveHikeRepository,
    private val trackingRepository: TrackingRepository,
    private val checkpointQrRepository: CheckpointQrRepository,
    private val trackingProgressRepository: TrackingProgressRepository,
    private val gpsCheckInRepository: GpsCheckInRepository,
    private val manualCheckInRepository: ManualCheckInRepository,
    private val trackingGpsRepository: TrackingGpsRepository,
    private val syncScheduler: SyncScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PelacakanJejakUiState())
    val uiState: StateFlow<PelacakanJejakUiState> = _uiState.asStateFlow()

    private var loadedPosts: List<TrackingPost> = emptyList()
    private val completedPostIds = mutableSetOf<Int>()
    private var currentToken: String = ""
    private var currentGateId: Int = 0
    private var currentBookingId: String = ""
    private var currentPendakiId: String = ""

    fun showNoActiveHike() {
        loadedPosts = emptyList()
        completedPostIds.clear()
        currentGateId = 0
        currentBookingId = ""
        _uiState.update {
            it.copy(
                isLoadingPosts = false,
                postCheckpoints = emptyList(),
                postErrorMessage = null,
                noActiveHikeMessage = "Belum ada pendakian aktif. Check-in tiket dulu untuk memakai Pelacakan Jejak.",
                isLoadingProgress = false,
                progressText = null,
                progressErrorMessage = null
            )
        }
    }

    /**
     * Titik masuk layar Jejak: ambil daftar booking (GET /mytiket) SEKALI, lalu pakai
     * untuk mengisi kartu tiket + menentukan pendakian aktif (status_booking = 6).
     */
    fun start(token: String) {
        currentToken = token
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTickets = true, noActiveHikeMessage = null) }
            activeHikeRepository.getBookings(token)
                .onSuccess { bookings ->
                    val activeBooking = bookings.firstOrNull { it.statusBooking == 6 }
                    _uiState.update {
                        it.copy(
                            isLoadingTickets = false,
                            activeTicket = TicketUiMapper.toActiveTicket(bookings),
                            finishedTickets = TicketUiMapper.toFinishedTickets(bookings),
                            days = TicketUiMapper.toDays(
                                activeBooking?.tanggalMasuk,
                                activeBooking?.tanggalKeluar
                            ),
                            periodeLabel = TicketUiMapper.toPeriodeLabel(activeBooking?.tanggalMasuk)
                        )
                    }
                    val hike = ActiveHikeRepository.toActiveHike(bookings)
                    if (hike == null) {
                        showNoActiveHike()
                    } else {
                        currentPendakiId = hike.pendakiIds.firstOrNull().orEmpty()
                        loadPosts(token, hike.gateId, hike.bookingId)
                    }
                }
                .onFailure { error ->
                    showNoActiveHike()
                    _uiState.update {
                        it.copy(
                            isLoadingTickets = false,
                            noActiveHikeMessage = error.message ?: "Gagal memuat data pendakian"
                        )
                    }
                }
        }
    }

    fun loadPosts(token: String, gateId: Int, bookingId: String = "") {
        currentToken = token
        currentGateId = gateId
        currentBookingId = bookingId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPosts = true, postErrorMessage = null, noActiveHikeMessage = null) }
            trackingRepository.getPosts(token, gateId)
                .onSuccess { posts ->
                    loadedPosts = posts
                    _uiState.update {
                        it.copy(
                            isLoadingPosts = false,
                            postCheckpoints = TrackingPostUiMapper.toCheckpointUi(posts, completedPostIds),
                            loadedPosts = posts,
                            postErrorMessage = null
                        )
                    }
                    if (bookingId.isNotBlank()) {
                        loadProgress(token, bookingId)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingPosts = false,
                            postErrorMessage = error.message ?: "Gagal mengambil data post"
                        )
                    }
                }
        }
    }

    fun loadProgress(token: String = currentToken, bookingId: String = currentBookingId) {
        if (token.isBlank() || bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProgress = true, progressErrorMessage = null) }
            trackingProgressRepository.getProgress(token, bookingId)
                .onSuccess { progress ->
                    val checkpoints = progress.hikers.firstOrNull()?.checkpoints.orEmpty()
                    completedPostIds.clear()
                    // Gabungan SEMUA anggota tim (bukan hanya pendaki pertama) supaya pos yang
                    // sudah di-check-in anggota lain tidak salah tampil "Belum check-in".
                    completedPostIds += progress.hikers
                        .flatMap { hiker -> hiker.checkpoints.filter { it.completed }.map { it.postId } }
                        .distinct()

                    val jumlahAnggota = progress.hikers.size
                    val totalSelesai = progress.hikers.sumOf { it.completed }
                    val totalCheckpoint = progress.hikers.sumOf { it.total }
                    val persenTim = if (totalCheckpoint > 0) {
                        (totalSelesai * 100) / totalCheckpoint
                    } else 0

                    _uiState.update {
                        it.copy(
                            isLoadingProgress = false,
                            postCheckpoints = if (checkpoints.isNotEmpty()) {
                                TrackingProgressUiMapper.toCheckpointUi(checkpoints, loadedPosts, progress.hikers)
                            } else {
                                TrackingPostUiMapper.toCheckpointUi(loadedPosts, completedPostIds)
                            },
                            progressText = if (jumlahAnggota > 1) {
                                "$jumlahAnggota anggota · $totalSelesai/$totalCheckpoint checkpoint tim ($persenTim%)"
                            } else {
                                progress.hikers.firstOrNull()?.let { hp ->
                                    "${hp.nama}: ${hp.completed}/${hp.total} checkpoint · ${hp.percentage}%"
                                } ?: "Progress belum tersedia"
                            },
                            progressHikers = progress.hikers,
                            progressErrorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingProgress = false,
                            progressErrorMessage = error.message ?: "Gagal mengambil progress pendakian"
                        )
                    }
                }
        }
    }

    fun onQrScanned(
        rawValue: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        altitude: Double? = null,
        accuracy: Double? = null,
        batteryLevel: Int? = null
    ) {
        val payload = CheckpointQrParser.parse(rawValue)
        if (payload == null) {
            _uiState.update {
                it.copy(qrScanMessage = null, qrScanErrorMessage = "Format QR tidak dikenali")
            }
            return
        }

        // Pos terdekat menurut deteksi GPS terakhir + nama pos dari QR, untuk memastikan
        // QR yang di-scan memang milik pos yang sedang didatangi (lihat validator).
        val namaPosQr = loadedPosts.firstOrNull { it.id == payload.postId }?.nama
        val namaPosTerdekat = when (val gate = uiState.value.gpsGateState) {
            is GpsGateState.DalamRadius -> gate.postName
            is GpsGateState.LuarRadius -> gate.postName
            else -> null
        }
        val validationError = CheckpointQrCheckInValidator.errorMessage(
            qrCodeValue = rawValue,
            latitude = latitude,
            longitude = longitude,
            qrPostId = payload.postId,
            qrPostName = namaPosQr,
            posTerdekatId = uiState.value.posTerdekatId,
            posTerdekatName = namaPosTerdekat
        )
        if (validationError != null) {
            _uiState.update {
                it.copy(qrScanMessage = null, qrScanErrorMessage = validationError)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingIn = true, qrScanMessage = null, qrScanErrorMessage = null) }

            // Anti-duplikasi: kalau pos ini sudah pernah tercatat check-in (mis. sebelumnya
            // tersimpan di antrean offline), jangan kirim ulang.
            if (currentBookingId.isNotBlank() && currentPendakiId.isNotBlank() &&
                trackingRepository.hasCheckedIn(currentBookingId, currentPendakiId, payload.postId)
            ) {
                _uiState.update {
                    it.copy(
                        isCheckingIn = false,
                        qrScanMessage = "Pos ini sudah tercatat check-in.",
                        qrScanErrorMessage = null
                    )
                }
                return@launch
            }

            checkpointQrRepository.checkIn(
                token = currentToken,
                qrCodeValue = rawValue ?: "",
                latitude = latitude,
                longitude = longitude
            )
                .onSuccess { data ->
                    completedPostIds += data.post.id
                    kirimTitikPosisi(latitude, longitude, altitude, accuracy, batteryLevel)
                    _uiState.update {
                        it.copy(
                            isCheckingIn = false,
                            postCheckpoints = TrackingPostUiMapper.toCheckpointUi(loadedPosts, completedPostIds),
                            progressText = "${data.progress.completed}/${data.progress.total} checkpoint selesai · ${data.progress.percentage}%",
                            qrScanMessage = "Check-in berhasil di ${data.post.nama} · Progress ${data.progress.percentage}%",
                            qrScanErrorMessage = null
                        )
                    }
                    if (currentBookingId.isNotBlank()) {
                        loadProgress()
                    }
                }
                .onFailure { error ->
                    // Sinyal gunung putus? Simpan ke antrean lokal — dikirim otomatis saat online.
                    if (adalahMasalahJaringan(error)) {
                        val tersimpan = simpanKeAntrean(
                            postId = payload.postId,
                            method = METHOD_QR,
                            latitude = latitude,
                            longitude = longitude,
                            altitude = altitude,
                            accuracy = accuracy,
                            batteryLevel = batteryLevel
                        )
                        _uiState.update {
                            it.copy(
                                isCheckingIn = false,
                                qrScanMessage = if (tersimpan) {
                                    "Tidak ada sinyal — check-in ${payload.postId} disimpan & akan dikirim otomatis."
                                } else null,
                                qrScanErrorMessage = if (tersimpan) {
                                    null
                                } else {
                                    error.message ?: "Check-in gagal"
                                }
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isCheckingIn = false,
                                qrScanMessage = null,
                                qrScanErrorMessage = error.message ?: "Check-in gagal"
                            )
                        }
                    }
                }
        }
    }

    fun clearQrMessage() {
        _uiState.update { it.copy(qrScanMessage = null, qrScanErrorMessage = null) }
    }

    /**
     * Cek kedekatan GPS ke pos — TIDAK otomatis check-in.
     * Jika [nearbyPost] != null setelah ini, tampilkan dialog konfirmasi di UI.
     *
     * Juga memperbarui [PelacakanJejakUiState.gpsGateState] yang mengunci tombol
     * check-in: hanya [GpsGateState.DalamRadius] yang boleh lanjut absen.
     *
     * @param silent true untuk deteksi otomatis (mis. saat layar pos baru dibuka):
     *   status gate tetap diperbarui, tapi pesan error tidak dimunculkan sebagai
     *   toast supaya tidak berulang setiap kali layar dibuka.
     */
    fun checkNearbyPostGps(
        latitude: Double,
        longitude: Double,
        accuracy: Double? = null,
        silent: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckingGps = true,
                    nearbyPost = null,
                    posTerdekatId = null,
                    gpsCheckErrorMessage = null,
                    gpsGateState = GpsGateState.Unknown
                )
            }
            gpsCheckInRepository.checkNearbyPost(currentToken, latitude, longitude, accuracy)
                .onSuccess { data ->
                    val nearest = data.nearestPost
                    _uiState.update {
                        it.copy(
                            isCheckingGps = false,
                            // Dialog konfirmasi HANYA muncul saat pendaki benar-benar menekan
                            // tombol "Check-in Pos Ini" (silent = false). Auto-deteksi saat layar
                            // pos dibuka (silent = true) cuma memperbarui status tombol.
                            nearbyPost = if (!silent && nearest.withinRadius) nearest else null,
                            posTerdekatId = nearest.id,
                            gpsGateState = GpsGateMapper.toGateState(nearest),
                            gpsCheckErrorMessage = if (!nearest.withinRadius && !silent) {
                                "Kamu belum berada dalam radius pos terdekat (${nearest.nama}). " +
                                        "Jarak: ${nearest.distanceMeters.toInt()} m, radius: ${nearest.radiusMeter} m."
                            } else null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCheckingGps = false,
                            nearbyPost = null,
                            posTerdekatId = null,
                            gpsGateState = GpsGateState.GpsOff,
                            gpsCheckErrorMessage = if (silent) null
                            else error.message ?: "Gagal cek posisi GPS"
                        )
                    }
                }
        }
    }

    /**
     * Dipanggil dari UI saat lokasi HP tidak tersedia (GPS mati / izin ditolak).
     * Mengunci tombol check-in dengan alasan GPS.
     *
     * @param silent true saat auto-deteksi: gate tetap dikunci, tapi pesan tidak
     *   dimunculkan sebagai toast (menghindari spam saat layar baru dibuka).
     */
    fun onLocationUnavailable(message: String? = null, silent: Boolean = false) {
        _uiState.update {
            it.copy(
                nearbyPost = null,
                posTerdekatId = null,
                gpsGateState = GpsGateState.GpsOff,
                gpsCheckErrorMessage = if (silent) null
                else message ?: "GPS tidak aktif. Aktifkan GPS untuk check-in."
            )
        }
    }

    /**
     * Check-in manual ke [postId]. Koordinat wajib ada — ambil dari GPS device sebelum memanggil ini.
     */
    fun checkInManual(
        postId: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double? = null,
        accuracy: Double? = null,
        batteryLevel: Int? = null
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isManualCheckingIn = true,
                    manualCheckInMessage = null,
                    manualCheckInErrorMessage = null
                )
            }
            manualCheckInRepository.checkIn(currentToken, postId, latitude, longitude)
                .onSuccess { data ->
                    completedPostIds += data.post.id
                    kirimTitikPosisi(latitude, longitude, altitude, accuracy, batteryLevel)
                    _uiState.update {
                        it.copy(
                            isManualCheckingIn = false,
                            nearbyPost = null,
                            posTerdekatId = null,
                            postCheckpoints = TrackingPostUiMapper.toCheckpointUi(loadedPosts, completedPostIds),
                            progressText = "${data.progress.completed}/${data.progress.total} checkpoint selesai · ${data.progress.percentage}%",
                            manualCheckInMessage = "Check-in manual berhasil di ${data.post.nama}" +
                                    if (data.isManualOverride) " (override — kamu jauh dari pos)" else "",
                            manualCheckInErrorMessage = null
                        )
                    }
                    if (currentBookingId.isNotBlank()) loadProgress()
                }
                .onFailure { error ->
                    if (adalahMasalahJaringan(error)) {
                        val tersimpan = simpanKeAntrean(
                            postId = postId,
                            method = METHOD_MANUAL,
                            latitude = latitude,
                            longitude = longitude,
                            altitude = altitude,
                            accuracy = accuracy,
                            batteryLevel = batteryLevel
                        )
                        _uiState.update {
                            it.copy(
                                isManualCheckingIn = false,
                                nearbyPost = null,
                                posTerdekatId = null,
                                manualCheckInMessage = if (tersimpan) {
                                    "Tidak ada sinyal — check-in manual disimpan & akan dikirim otomatis."
                                } else null,
                                manualCheckInErrorMessage = if (tersimpan) {
                                    null
                                } else {
                                    error.message ?: "Check-in manual gagal"
                                }
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isManualCheckingIn = false,
                                manualCheckInMessage = null,
                                manualCheckInErrorMessage = error.message ?: "Check-in manual gagal"
                            )
                        }
                    }
                }
        }
    }

    fun clearGpsAndManualMessages() {
        _uiState.update {
            it.copy(
                nearbyPost = null,
                posTerdekatId = null,
                gpsCheckErrorMessage = null,
                manualCheckInMessage = null,
                manualCheckInErrorMessage = null
            )
        }
    }

    /**
     * Kirim 1 titik posisi ke gk_tracking setelah check-in BERHASIL.
     *
     * Tembak-lalu-lupakan: tidak menunggu hasil, tidak pernah menampilkan error,
     * tidak menyentuh isCheckingIn / isManualCheckingIn. Posisi = data pelengkap;
     * check-in = jalur keselamatan yang tidak boleh terganggu.
     */
    private fun kirimTitikPosisi(
        latitude: Double?,
        longitude: Double?,
        altitude: Double?,
        accuracy: Double?,
        batteryLevel: Int?
    ) {
        if (latitude == null || longitude == null) return
        viewModelScope.launch {
            runCatching {
                trackingGpsRepository.kirimTitik(
                    token = currentToken,
                    latitude = latitude,
                    longitude = longitude,
                    altitude = altitude,
                    accuracy = accuracy,
                    batteryLevel = batteryLevel
                )
            }
        }
    }

    /**
     * Simpan check-in yang gagal karena jaringan ke antrean lokal (Room),
     * lalu jadwalkan worker untuk mengirimnya saat internet tersedia.
     *
     * @return true bila berhasil masuk antrean.
     */
    private suspend fun simpanKeAntrean(
        postId: Int,
        method: String,
        latitude: Double?,
        longitude: Double?,
        altitude: Double? = null,
        accuracy: Double? = null,
        batteryLevel: Int? = null
    ): Boolean {
        if (currentBookingId.isBlank() || currentPendakiId.isBlank()) return false

        val log = CheckpointLogEntity(
            localId = "${currentBookingId}_${currentPendakiId}_$postId",
            serverId = null,
            bookingId = currentBookingId,
            pendakiId = currentPendakiId,
            postId = postId,
            method = method,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            accuracy = accuracy,
            batteryLevel = batteryLevel,
            checkedAt = System.currentTimeMillis()
        )
        val tersimpan = runCatching { trackingRepository.saveCheckpointPending(log) }.isSuccess

        if (tersimpan) {
            syncScheduler?.scheduleCheckpointSync()
            // Optimistis: tampilkan pos ini sebagai selesai di timeline.
            completedPostIds += postId
            _uiState.update {
                it.copy(postCheckpoints = TrackingPostUiMapper.toCheckpointUi(loadedPosts, completedPostIds))
            }
        }
        return tersimpan
    }

    private fun adalahMasalahJaringan(error: Throwable): Boolean =
        error is java.io.IOException ||
                error.cause is java.io.IOException ||
                error.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                error.message?.contains("Failed to connect", ignoreCase = true) == true ||
                error.message?.contains("timeout", ignoreCase = true) == true

    private companion object {
        const val METHOD_QR = "qr"
        const val METHOD_MANUAL = "manual"
    }
}

internal class PelacakanJejakViewModelFactory(
    private val activeHikeRepository: ActiveHikeRepository,
    private val trackingRepository: TrackingRepository,
    private val checkpointQrRepository: CheckpointQrRepository,
    private val trackingProgressRepository: TrackingProgressRepository,
    private val gpsCheckInRepository: GpsCheckInRepository,
    private val manualCheckInRepository: ManualCheckInRepository,
    private val trackingGpsRepository: TrackingGpsRepository,
    private val syncScheduler: SyncScheduler? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PelacakanJejakViewModel::class.java)) {
            return PelacakanJejakViewModel(
                activeHikeRepository,
                trackingRepository,
                checkpointQrRepository,
                trackingProgressRepository,
                gpsCheckInRepository,
                manualCheckInRepository,
                trackingGpsRepository,
                syncScheduler
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
