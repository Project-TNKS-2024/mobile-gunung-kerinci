package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dicoding.gunungkerinci.data.local.checkpoint.CheckpointLogEntity
import com.dicoding.gunungkerinci.data.local.checkpoint.checkpointLocalId
import com.dicoding.gunungkerinci.data.local.sync.SyncStatus
import com.dicoding.gunungkerinci.data.repository.ActiveHikeRepository
import com.dicoding.gunungkerinci.data.repository.CheckpointQrRepository
import com.dicoding.gunungkerinci.data.repository.GpsCheckInRepository
import com.dicoding.gunungkerinci.data.repository.ManualCheckInRepository
import com.dicoding.gunungkerinci.data.repository.TrackingGpsRepository
import com.dicoding.gunungkerinci.data.repository.TrackingProgressRepository
import com.dicoding.gunungkerinci.data.repository.TrackingRepository
import com.dicoding.gunungkerinci.data.repository.isoWib
import com.dicoding.gunungkerinci.data.sync.SyncScheduler
import com.dicoding.gunungkerinci.model.CheckpointApiException
import com.dicoding.gunungkerinci.model.GpsNearestPost
import com.dicoding.gunungkerinci.model.TrackingPhase
import com.dicoding.gunungkerinci.model.TrackingPost
import com.dicoding.gunungkerinci.model.TrackingProgressV2Hiker
import com.dicoding.gunungkerinci.model.TrackingVisitV2
import com.dicoding.gunungkerinci.model.PhaseProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

internal data class PelacakanJejakUiState(
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
    // Visit yang sedang dibuka untuk check-in (identitas kunjungan, bukan postId saja).
    val expectedVisitKey: String? = null,
    val expectedPhase: String? = null,
    val expectedPostId: Int? = null,
    val checkedInVisitKey: String? = null,
    // GPS check state
    val isCheckingGps: Boolean = false,
    val nearbyPost: GpsNearestPost? = null,
    val posTerdekatId: Int? = null,
    val gpsCheckErrorMessage: String? = null,
    val gpsGateState: GpsGateState = GpsGateState.Unknown,
    // Manual check-in state
    val isManualCheckingIn: Boolean = false,
    val manualCheckInMessage: String? = null,
    val manualCheckInErrorMessage: String? = null,
    // Data progres v2 — dipakai timeline dua fase dan daftar anggota.
    val progressHikers: List<TrackingProgressV2Hiker> = emptyList(),
    val localMemberMarks: List<LocalMemberMark> = emptyList(),
    val isKetua: Boolean? = null,
    val myPendakiId: String? = null,
    // Fase efektif pendaki yang login + tujuan berikutnya.
    val trackingPhase: String = TrackingPhase.NAIK.serialized,
    val nextVisitKey: String? = null,
    // Penandaan kehadiran anggota oleh ketua tim.
    val isMarkingMember: Boolean = false,
    val processingMemberId: String? = null,
    val memberNotes: List<com.dicoding.gunungkerinci.model.CheckpointMemberNote> = emptyList(),
    val savingMemberNoteKey: String? = null,
    val memberNoteErrorKey: String? = null,
    val memberNoteError: String? = null,
    val memberMarkMessage: String? = null,
    val memberMarkErrorMessage: String? = null,
    // Daftar pos jalur (untuk jarak antar-pos di layar Location Detail + deteksi puncak).
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
    private var serverHikers: List<TrackingProgressV2Hiker> = emptyList()
    private var currentToken: String = ""
    private var currentGateId: Int = 0
    private var currentBookingId: String = ""
    private var currentPendakiId: String = ""

    fun showNoActiveHike() {
        loadedPosts = emptyList()
        serverHikers = emptyList()
        currentGateId = 0
        currentBookingId = ""
        currentPendakiId = ""
        _uiState.update {
            it.copy(
                isLoadingPosts = false,
                postCheckpoints = emptyList(),
                postErrorMessage = null,
                noActiveHikeMessage = "Belum ada pendakian aktif. Check-in tiket dulu untuk memakai Pelacakan Jejak.",
                isLoadingProgress = false,
                progressText = null,
                progressErrorMessage = null,
                isKetua = null,
                myPendakiId = null,
                progressHikers = emptyList(),
                localMemberMarks = emptyList(),
                loadedPosts = emptyList(),
                trackingPhase = TrackingPhase.NAIK.serialized,
                nextVisitKey = null,
                expectedVisitKey = null,
                expectedPhase = null,
                expectedPostId = null,
                checkedInVisitKey = null
            )
        }
    }

    fun start(token: String) {
        currentToken = token
        if (_uiState.value.isLoadingTickets) return
        _uiState.update { it.copy(isLoadingTickets = true) }
        viewModelScope.launch {
            try {
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
                                ).let { days ->
                                    val selectedDate = it.days.firstOrNull { day -> day.selected }?.isoDate
                                    val target = selectedDate?.takeIf { date -> days.any { day -> day.isoDate == date } }
                                        ?: JejakTimelineMapper.today().takeIf { date -> days.any { day -> day.isoDate == date } }
                                        ?: days.firstOrNull()?.isoDate
                                    days.map { day -> day.copy(selected = day.isoDate == target) }
                                },
                                periodeLabel = TicketUiMapper.toPeriodeLabel(activeBooking?.tanggalMasuk)
                            )
                        }
                        val hike = ActiveHikeRepository.toActiveHike(bookings)
                        if (hike == null) {
                            showNoActiveHike()
                        } else {
                            val identitas = activeHikeRepository.getMyIdentity(token)
                            currentPendakiId = ActiveHikeRepository.pilihPendakiSaya(
                                hike.pendakiIds,
                                identitas.pendakiIds
                            )
                            val ketua = if (hike.ketuaUserId != null && identitas.userId != null) {
                                hike.ketuaUserId == identitas.userId
                            } else null
                            _uiState.update {
                                it.copy(isKetua = ketua, myPendakiId = currentPendakiId.ifBlank { null })
                            }
                            loadPosts(token, hike.gateId, hike.bookingId)
                        }
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _uiState.update {
                            it.copy(
                                isLoadingTickets = false,
                                noActiveHikeMessage = if (it.activeTicket == null) error.message ?: "Gagal memuat data pendakian" else null,
                                postErrorMessage = if (it.activeTicket != null) error.message ?: "Gagal memperbarui pendakian" else it.postErrorMessage
                            )
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(noActiveHikeMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isLoadingTickets = false) }
            }
        }
    }

    fun selectDay(index: Int) {
        _uiState.update { state ->
            if (index !in state.days.indices) state else state.copy(
                days = state.days.mapIndexed { dayIndex, day -> day.copy(selected = dayIndex == index) }
            )
        }
    }

    fun loadPosts(token: String, gateId: Int, bookingId: String = "") {
        if (_uiState.value.isLoadingPosts) return
        currentToken = token
        currentGateId = gateId
        if (currentBookingId != bookingId) {
            serverHikers = emptyList()
            _uiState.update {
                it.copy(
                    postCheckpoints = emptyList(), progressHikers = emptyList(),
                    localMemberMarks = emptyList(), expectedVisitKey = null,
                    expectedPhase = null, expectedPostId = null, checkedInVisitKey = null
                )
            }
        }
        currentBookingId = bookingId
        _uiState.update { it.copy(isLoadingPosts = true) }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingPosts = true, postErrorMessage = null, noActiveHikeMessage = null) }
                trackingRepository.getPosts(token, gateId)
                    .onSuccess { posts ->
                        loadedPosts = posts
                        _uiState.update {
                            it.copy(loadedPosts = posts, postErrorMessage = null)
                        }
                        refreshLocalMemberMarks()
                        rebuildTimeline()
                        if (bookingId.isNotBlank()) {
                            loadProgress(token, bookingId)
                        } else {
                            rebuildTimeline()
                        }
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _uiState.update {
                            it.copy(
                                postErrorMessage = error.message ?: "Gagal mengambil data post"
                            )
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(postErrorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isLoadingPosts = false) }
            }
        }
    }

    fun loadProgress(token: String = currentToken, bookingId: String = currentBookingId) {
        if (token.isBlank() || bookingId.isBlank()) return
        if (_uiState.value.isLoadingProgress) return
        _uiState.update { it.copy(isLoadingProgress = true) }
        viewModelScope.launch {
            try {
                val dibuka = trackingRepository.resetRetriableFailures(bookingId)
                if (dibuka > 0) syncScheduler?.scheduleCheckpointSync(bookingId)

                _uiState.update { it.copy(isLoadingProgress = true, progressErrorMessage = null) }
                trackingProgressRepository.getProgressV2(token, bookingId)
                    .onSuccess { progress ->
                        progress.currentPendakiId?.takeIf { it.isNotBlank() }?.let { currentPendakiId = it }
                        serverHikers = progress.hikers
                        _uiState.update { it.copy(memberNotes = progress.memberNotes.orEmpty()) }
                        if (!progress.fromCache) {
                            for (hiker in progress.hikers) {
                                trackingRepository.removeStaleServerLogs(bookingId, hiker.pendakiId,
                                    hiker.visits.filter { it.completed }.mapNotNull { it.checkpointLogId })
                                for (visit in hiker.visits.filter { it.completed }) {
                                    val serverId = visit.checkpointLogId ?: continue
                                    trackingRepository.saveServerLogId(
                                        bookingId, hiker.pendakiId, visit.postId, visit.phase, serverId,
                                        visit.method ?: METHOD_QR, isSummit = visit.isSummit,
                                        checkedAt = JejakTimelineMapper.millis(visit.checkedAt) ?: continue
                                    )
                                }
                            }
                        }
                        _uiState.update {
                            it.copy(
                                progressHikers = progress.hikers,
                                myPendakiId = currentPendakiId.ifBlank { null },
                                progressErrorMessage = null
                            )
                        }
                        refreshLocalMemberMarks()
                        rebuildTimeline()
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        val message = if (error is CheckpointApiException && error.code == "ROUTE_CONFIGURATION_INVALID") {
                            "Jalur belum dikonfigurasi untuk fitur turun: ${error.message}"
                        } else {
                            error.message ?: "Gagal mengambil progress pendakian"
                        }
                        _uiState.update {
                            it.copy(progressErrorMessage = message)
                        }
                        rebuildTimeline()
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(progressErrorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isLoadingProgress = false) }
            }
        }
    }

    fun prepareCheckpointCheckIn(visitKey: String, phase: String, postId: Int) {
        _uiState.update {
            it.copy(
                expectedVisitKey = visitKey,
                expectedPhase = phase,
                expectedPostId = postId,
                checkedInVisitKey = null,
                nearbyPost = null,
                posTerdekatId = null,
                gpsGateState = GpsGateState.Unknown,
                gpsCheckErrorMessage = null
            )
        }
    }

    fun clearCheckInNavigation() {
        _uiState.update { it.copy(checkedInVisitKey = null) }
    }

    fun onQrScanned(
        rawValue: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        altitude: Double? = null,
        accuracy: Double? = null,
        batteryLevel: Int? = null
    ) {
        if (_uiState.value.isCheckingIn || _uiState.value.isManualCheckingIn) return
        val payload = CheckpointQrParser.parse(rawValue)
        if (payload == null) {
            _uiState.update { it.copy(qrScanMessage = null, qrScanErrorMessage = "Format QR tidak dikenali") }
            return
        }

        val expectedPostId = _uiState.value.expectedPostId
        if (expectedPostId != null && payload.postId != expectedPostId) {
            _uiState.update { it.copy(qrScanMessage = null, qrScanErrorMessage = "QR tidak sesuai dengan pos yang dipilih.") }
            return
        }

        val phase = _uiState.value.expectedPhase ?: TrackingPhase.NAIK.serialized
        if (phase != _uiState.value.trackingPhase) {
            _uiState.update { it.copy(qrScanErrorMessage = "Fase check-in tidak sesuai perjalanan saat ini.") }
            return
        }

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
            _uiState.update { it.copy(qrScanMessage = null, qrScanErrorMessage = validationError) }
            return
        }

        val clientEventId = UUID.randomUUID().toString()
        val checkedAt = System.currentTimeMillis()
        val recordedAt = isoWib(checkedAt)

        _uiState.update { it.copy(isCheckingIn = true) }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCheckingIn = true, qrScanMessage = null, qrScanErrorMessage = null) }

                if (currentBookingId.isNotBlank() && currentPendakiId.isNotBlank() &&
                    trackingRepository.hasCheckedIn(currentBookingId, currentPendakiId, payload.postId, phase)
                ) {
                    _uiState.update {
                        it.copy(
                            isCheckingIn = false,
                            qrScanMessage = "Pos ini sudah tercatat check-in.",
                            checkedInVisitKey = "$phase:${payload.postId}",
                            qrScanErrorMessage = null
                        )
                    }
                    return@launch
                }

                checkpointQrRepository.checkInV2(
                    token = currentToken,
                    qrCodeValue = rawValue ?: "",
                    phase = phase,
                    clientEventId = clientEventId,
                    recordedAt = recordedAt,
                    latitude = latitude,
                    longitude = longitude
                )
                    .onSuccess { data ->
                        kirimTitikPosisi(latitude, longitude, altitude, accuracy, batteryLevel)
                        if (currentBookingId.isNotBlank() && currentPendakiId.isNotBlank()) {
                            trackingRepository.saveServerLogId(
                                bookingId = currentBookingId,
                                pendakiId = currentPendakiId,
                                postId = data.post.id,
                                phase = data.phase,
                                serverId = data.checkpointLogId,
                                method = METHOD_QR,
                                clientEventId = clientEventId,
                                isSummit = loadedPosts.firstOrNull { it.id == data.post.id }?.isSummit == true,
                                checkedAt = JejakTimelineMapper.millis(data.checkedAt) ?: checkedAt
                            )
                        }
                        _uiState.update {
                            it.copy(
                                checkedInVisitKey = data.visitKey ?: "$phase:${data.post.id}",
                                trackingPhase = data.trackingPhase ?: it.trackingPhase,
                                qrScanMessage = "Check-in berhasil di ${data.post.nama}",
                                qrScanErrorMessage = null
                            )
                        }
                        rebuildTimeline()
                        if (currentBookingId.isNotBlank()) loadProgress()
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        if (shouldQueueCheckIn(error, phase, currentPendakiId, payload.postId)) {
                            val tersimpan = simpanKeAntrean(
                                postId = payload.postId,
                                phase = phase,
                                method = METHOD_QR,
                                latitude = latitude,
                                longitude = longitude,
                                altitude = altitude,
                                accuracy = accuracy,
                                batteryLevel = batteryLevel,
                                clientEventId = clientEventId,
                                checkedAt = checkedAt
                            )
                            _uiState.update {
                                it.copy(
                                    qrScanMessage = if (tersimpan) "Check-in disimpan dan akan disinkronkan otomatis." else null,
                                    checkedInVisitKey = if (tersimpan) "$phase:${payload.postId}" else null,
                                    qrScanErrorMessage = if (tersimpan) null else error.message ?: "Check-in gagal"
                                )
                            }
                        } else {
                            val msg = pesanKonflik(error)
                            _uiState.update { it.copy(qrScanMessage = null, qrScanErrorMessage = msg) }
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(qrScanErrorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isCheckingIn = false) }
            }
        }
    }

    fun clearQrMessage() {
        _uiState.update { it.copy(qrScanMessage = null, qrScanErrorMessage = null) }
    }

    fun tandaiKehadiranAnggota(pendakiId: String, checked: Boolean, postId: Int?, phase: String?) {
        if (_uiState.value.isMarkingMember) return
        if (postId == null || phase == null) {
            _uiState.update { it.copy(memberMarkMessage = null, memberMarkErrorMessage = "Pos belum diketahui") }
            return
        }
        if (pendakiId.isBlank()) {
            _uiState.update { it.copy(memberMarkMessage = null, memberMarkErrorMessage = "Anggota tidak dikenali") }
            return
        }

        val pendingCancellation = !checked && _uiState.value.localMemberMarks.any {
            it.postId == postId && it.pendakiId == pendakiId && it.phase == phase && it.pending
        }
        val ketuaAda = TrackingProgressUiMapper.ketuaAdaDiPosIni(
            postId, phase, currentPendakiId, _uiState.value.progressHikers, _uiState.value.gpsGateState
        )
        if (pendakiId == currentPendakiId || (!pendingCancellation && !ketuaAda)) {
            _uiState.update {
                it.copy(
                    memberMarkMessage = null,
                    memberMarkErrorMessage = if (pendakiId == currentPendakiId) {
                        "Gunakan scan QR atau check-in GPS untuk kehadiran sendiri."
                    } else {
                        "Hanya ketua tim yang sudah check-in di pos ini pada fase yang sama yang dapat menandai anggota."
                    }
                )
            }
            return
        }

        if (!checked) {
            batalkanKehadiranAnggota(pendakiId, postId, phase)
            return
        }

        _uiState.update { it.copy(isMarkingMember = true) }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isMarkingMember = true, processingMemberId = pendakiId, memberMarkMessage = null, memberMarkErrorMessage = null) }

                if (currentBookingId.isNotBlank() &&
                    trackingRepository.hasCheckedIn(currentBookingId, pendakiId, postId, phase)
                ) {
                    _uiState.update {
                        it.copy(memberMarkMessage = "Anggota ini sudah tercatat di pos ini.", memberMarkErrorMessage = null)
                    }
                    return@launch
                }

                val clientEventId = UUID.randomUUID().toString()
                val checkedAt = System.currentTimeMillis()
                val recordedAt = isoWib(checkedAt)
                val namaPos = loadedPosts.firstOrNull { it.id == postId }?.nama

                checkpointQrRepository.checkInV2(
                    token = currentToken,
                    qrCodeValue = "$QR_PREFIX$postId",
                    phase = phase,
                    clientEventId = clientEventId,
                    recordedAt = recordedAt,
                    pendakiId = pendakiId
                )
                    .onSuccess { data ->
                        if (currentBookingId.isNotBlank()) {
                            trackingRepository.saveServerLogId(
                                bookingId = currentBookingId,
                                pendakiId = pendakiId,
                                postId = data.post.id,
                                phase = data.phase,
                                serverId = data.checkpointLogId,
                                method = METHOD_QR,
                                clientEventId = clientEventId,
                                isSummit = loadedPosts.firstOrNull { it.id == data.post.id }?.isSummit == true,
                                checkedAt = JejakTimelineMapper.millis(data.checkedAt) ?: checkedAt
                            )
                        }
                        _uiState.update {
                            it.copy(memberMarkMessage = "Kehadiran tercatat di ${data.post.nama}${namaPos?.let { n -> " · $n" } ?: ""}", memberMarkErrorMessage = null)
                        }
                        if (currentBookingId.isNotBlank()) loadProgress()
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        if (shouldQueueCheckIn(error, phase, pendakiId, postId)) {
                            val namaAnggota = serverHikers.firstOrNull { it.pendakiId == pendakiId }?.nama
                            val tersimpan = simpanKeAntrean(
                                postId = postId,
                                phase = phase,
                                method = METHOD_QR,
                                latitude = null,
                                longitude = null,
                                pendakiId = pendakiId,
                                tandaiTimeline = false,
                                clientEventId = clientEventId,
                                checkedAt = checkedAt
                            )
                            _uiState.update {
                                it.copy(
                                    memberMarkMessage = if (tersimpan) "Kehadiran ${namaAnggota ?: "anggota"} disimpan dan akan disinkronkan otomatis." else null,
                                    memberMarkErrorMessage = if (tersimpan) null else error.message ?: "Gagal mencatat kehadiran anggota"
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(memberMarkMessage = null, memberMarkErrorMessage = pesanKonflik(error))
                            }
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(memberMarkErrorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isMarkingMember = false, processingMemberId = null) }
            }
        }
    }

    fun saveMemberNote(pendakiId: String, note: String, postId: Int?, phase: String?) {
        val state = _uiState.value
        if (state.savingMemberNoteKey != null || state.isMarkingMember || postId == null || phase == null || currentBookingId.isBlank()) return
        val key = "$phase:$postId:$pendakiId"
        val bookingId = currentBookingId
        val token = currentToken
        _uiState.update { it.copy(savingMemberNoteKey = key, memberNoteErrorKey = null, memberNoteError = null) }
        viewModelScope.launch {
            try {
                val request = com.dicoding.gunungkerinci.model.SaveCheckpointMemberNoteRequest(pendakiId, postId, phase, note.trim())
                trackingProgressRepository.saveMemberNote(token, bookingId, request)
                    .onSuccess { saved ->
                        if (currentBookingId == bookingId) _uiState.update {
                            it.copy(memberNotes = it.memberNotes.filter { old -> old.key != saved.key } + saved)
                        }
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        if (currentBookingId == bookingId) _uiState.update {
                            it.copy(memberNoteErrorKey = key, memberNoteError = error.message ?: "Catatan belum tersimpan. Coba simpan lagi.")
                        }
                    }
            } finally {
                if (currentBookingId == bookingId) _uiState.update { it.copy(savingMemberNoteKey = null) }
            }
        }
    }

    private fun batalkanKehadiranAnggota(pendakiId: String, postId: Int, phase: String) {
        _uiState.update { it.copy(isMarkingMember = true) }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isMarkingMember = true, processingMemberId = pendakiId, memberMarkMessage = null, memberMarkErrorMessage = null) }

                val localLog = if (currentBookingId.isNotBlank()) {
                    trackingRepository.getCheckpointLog(currentBookingId, pendakiId, postId, phase)
                } else null
                val serverVisit = serverHikers.firstOrNull { it.pendakiId == pendakiId }
                    ?.visits?.firstOrNull { it.postId == postId && it.phase == phase && it.completed }
                val logId = serverVisit?.checkpointLogId ?: localLog?.serverId

                if (localLog != null && logId == null) {
                    // Timeout/SYNCING bisa sudah diterima server. Jangan menghapus bukti lokal sepihak.
                    if (localLog.syncStatus != SyncStatus.REJECTED.name) {
                        _uiState.update {
                            it.copy(memberMarkMessage = null, memberMarkErrorMessage = "Sinkronkan kehadiran dahulu sebelum membatalkannya.")
                        }
                        return@launch
                    }
                    trackingRepository.deleteCheckInLocal(currentBookingId, pendakiId, postId, phase)
                    _uiState.update {
                        it.copy(memberMarkMessage = "Kehadiran dibatalkan (belum terkirim ke server)", memberMarkErrorMessage = null)
                    }
                    refreshLocalMemberMarks()
                    rebuildTimeline()
                    return@launch
                }

                if (logId == null) {
                    _uiState.update {
                        it.copy(memberMarkMessage = null, memberMarkErrorMessage = "Tidak dapat membatalkan: catatan kehadiran tidak ditemukan di perangkat ini")
                    }
                    return@launch
                }

                checkpointQrRepository.cancelCheckInV2(currentToken, logId)
                    .onSuccess { data ->
                        if (currentBookingId.isNotBlank()) {
                            trackingRepository.deleteCheckInLocal(currentBookingId, pendakiId, postId, phase)
                            trackingProgressRepository.removeCachedVisit(currentToken, currentBookingId, pendakiId, "$phase:$postId")
                        }
                        serverHikers = serverHikers.map { hiker ->
                            if (hiker.pendakiId != pendakiId) hiker else hiker.copy(visits = hiker.visits.map { visit ->
                                if (visit.visitKey == "$phase:$postId") visit.copy(
                                    completed = false, checkedAt = null, checkpointLogId = null, method = null
                                ) else visit
                            })
                        }
                        refreshLocalMemberMarks()
                        rebuildTimeline()
                        _uiState.update {
                            it.copy(memberMarkMessage = "Kehadiran dibatalkan di ${data.post.nama}", memberMarkErrorMessage = null)
                        }
                        if (currentBookingId.isNotBlank()) loadProgress()
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _uiState.update {
                            it.copy(memberMarkMessage = null, memberMarkErrorMessage = error.message ?: "Gagal membatalkan kehadiran")
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(memberMarkErrorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isMarkingMember = false, processingMemberId = null) }
            }
        }
    }

    fun checkNearbyPostGps(
        latitude: Double,
        longitude: Double,
        accuracy: Double? = null,
        silent: Boolean = false
    ) {
        if (_uiState.value.isCheckingGps) return
        _uiState.update { it.copy(isCheckingGps = true) }
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isCheckingGps = true, nearbyPost = null, posTerdekatId = null, gpsCheckErrorMessage = null, gpsGateState = GpsGateState.Unknown)
                }
                gpsCheckInRepository.checkNearbyPost(currentToken, latitude, longitude, accuracy)
                    .onSuccess { data ->
                        val nearest = data.nearestPost
                        _uiState.update {
                            it.copy(
                                isCheckingGps = false,
                                nearbyPost = if (!silent && nearest.withinRadius) nearest else null,
                                posTerdekatId = nearest.id,
                                gpsGateState = GpsGateMapper.toGateState(nearest),
                                gpsCheckErrorMessage = if (!nearest.withinRadius && !silent) {
                                    "Kamu belum berada dalam radius pos terdekat (${nearest.nama}). Jarak: ${nearest.distanceMeters.toInt()} m, radius: ${nearest.radiusMeter} m."
                                } else null
                            )
                        }
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _uiState.update {
                            it.copy(
                                isCheckingGps = false, nearbyPost = null, posTerdekatId = null,
                                gpsGateState = GpsGateState.GagalCek,
                                gpsCheckErrorMessage = if (silent) null
                                else if (adalahMasalahJaringan(error)) "Tidak ada jaringan. Cek posisi butuh internet, pakai Scan QR atau coba lagi."
                                else error.message ?: "Gagal cek posisi GPS"
                            )
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(gpsCheckErrorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isCheckingGps = false) }
            }
        }
    }

    fun onLocationUnavailable(message: String? = null, silent: Boolean = false) {
        _uiState.update {
            it.copy(
                nearbyPost = null, posTerdekatId = null, gpsGateState = GpsGateState.GpsOff,
                gpsCheckErrorMessage = if (silent) null else message ?: "GPS tidak aktif. Aktifkan GPS untuk check-in."
            )
        }
    }

    fun checkInManual(
        postId: Int,
        latitude: Double,
        longitude: Double,
        altitude: Double? = null,
        accuracy: Double? = null,
        batteryLevel: Int? = null
    ) {
        if (_uiState.value.isManualCheckingIn || _uiState.value.isCheckingIn) return
        val expectedPostId = _uiState.value.expectedPostId
        if (expectedPostId != null && postId != expectedPostId) {
            _uiState.update { it.copy(manualCheckInErrorMessage = "Pos tidak sesuai dengan pilihan check-in.") }
            return
        }
        val phase = _uiState.value.expectedPhase ?: TrackingPhase.NAIK.serialized
        if (phase != _uiState.value.trackingPhase) {
            _uiState.update { it.copy(manualCheckInErrorMessage = "Fase check-in tidak sesuai perjalanan saat ini.") }
            return
        }

        _uiState.update { it.copy(isManualCheckingIn = true) }
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(isManualCheckingIn = true, manualCheckInMessage = null, manualCheckInErrorMessage = null)
                }
                val clientEventId = UUID.randomUUID().toString()
                val checkedAt = System.currentTimeMillis()
                val recordedAt = isoWib(checkedAt)
                manualCheckInRepository.checkInV2(
                    currentToken, postId, phase, clientEventId, recordedAt, latitude, longitude
                )
                    .onSuccess { data ->
                        kirimTitikPosisi(latitude, longitude, altitude, accuracy, batteryLevel)
                        if (currentBookingId.isNotBlank() && currentPendakiId.isNotBlank()) {
                            trackingRepository.saveServerLogId(
                                currentBookingId, currentPendakiId, data.post.id, data.phase,
                                data.checkpointLogId, METHOD_MANUAL, clientEventId,
                                loadedPosts.firstOrNull { it.id == data.post.id }?.isSummit == true,
                                JejakTimelineMapper.millis(data.checkedAt) ?: checkedAt
                            )
                        }
                        _uiState.update {
                            it.copy(
                                nearbyPost = null,
                                checkedInVisitKey = data.visitKey ?: "$phase:$postId",
                                posTerdekatId = null,
                                trackingPhase = data.trackingPhase ?: it.trackingPhase,
                                manualCheckInMessage = "Check-in manual berhasil di ${data.post.nama}" + if (data.isManualOverride) " (override, kamu jauh dari pos)" else "",
                                manualCheckInErrorMessage = null
                            )
                        }
                        rebuildTimeline()
                        if (currentBookingId.isNotBlank()) loadProgress()
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        if (shouldQueueCheckIn(error, phase, currentPendakiId, postId)) {
                            val tersimpan = simpanKeAntrean(
                                postId = postId, phase = phase, method = METHOD_MANUAL,
                                latitude = latitude, longitude = longitude, altitude = altitude,
                                accuracy = accuracy, batteryLevel = batteryLevel, clientEventId = clientEventId,
                                checkedAt = checkedAt
                            )
                            _uiState.update {
                                it.copy(
                                    nearbyPost = null, posTerdekatId = null,
                                    manualCheckInMessage = if (tersimpan) "Check-in manual disimpan dan akan disinkronkan otomatis." else null,
                                    checkedInVisitKey = if (tersimpan) "$phase:$postId" else null,
                                    manualCheckInErrorMessage = if (tersimpan) null else error.message ?: "Check-in manual gagal"
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(manualCheckInMessage = null, manualCheckInErrorMessage = pesanKonflik(error))
                            }
                        }
                    }

            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(manualCheckInErrorMessage = error.message ?: "Proses gagal. Coba lagi.") }
            } finally {
                _uiState.update { it.copy(isManualCheckingIn = false) }
            }
        }
    }

    fun clearGpsAndManualMessages() {
        _uiState.update {
            it.copy(
                nearbyPost = null, posTerdekatId = null, gpsCheckErrorMessage = null,
                manualCheckInMessage = null, manualCheckInErrorMessage = null
            )
        }
    }

    private fun kirimTitikPosisi(
        latitude: Double?, longitude: Double?, altitude: Double?, accuracy: Double?, batteryLevel: Int?
    ) {
        if (latitude == null || longitude == null) return
        viewModelScope.launch {
            runCatching {
                trackingGpsRepository.kirimTitik(
                    token = currentToken, latitude = latitude, longitude = longitude,
                    altitude = altitude, accuracy = accuracy, batteryLevel = batteryLevel
                )
            }
        }
    }

    private suspend fun simpanKeAntrean(
        postId: Int,
        phase: String,
        method: String,
        latitude: Double?,
        longitude: Double?,
        altitude: Double? = null,
        accuracy: Double? = null,
        batteryLevel: Int? = null,
        pendakiId: String? = null,
        tandaiTimeline: Boolean = true,
        clientEventId: String? = null,
        checkedAt: Long = System.currentTimeMillis()
    ): Boolean {
        val targetPendaki = pendakiId ?: currentPendakiId
        if (currentBookingId.isBlank() || targetPendaki.isBlank()) return false

        val isSummit = loadedPosts.firstOrNull { it.id == postId }?.isSummit == true
        val eventId = clientEventId ?: UUID.randomUUID().toString()
        val log = CheckpointLogEntity(
            localId = checkpointLocalId(currentBookingId, targetPendaki, postId, phase),
            serverId = null,
            bookingId = currentBookingId,
            pendakiId = targetPendaki,
            postId = postId,
            phase = phase,
            clientEventId = eventId,
            isSummit = isSummit,
            method = method,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            accuracy = accuracy,
            batteryLevel = batteryLevel,
            checkedAt = checkedAt
        )
        val tersimpan = runCatching { trackingRepository.saveCheckpointPending(log) }.isSuccess
        if (tersimpan) {
            syncScheduler?.scheduleCheckpointSync(currentBookingId)
            if (tandaiTimeline) {
                _uiState.update { it.copy(trackingPhase = if (isSummit && phase == TrackingPhase.NAIK.serialized) TrackingPhase.TURUN.serialized else it.trackingPhase) }
                rebuildTimeline()
                refreshLocalMemberMarks()
            } else {
                refreshLocalMemberMarks()
            }
        }
        return tersimpan
    }

    private suspend fun refreshLocalMemberMarks() {
        if (currentBookingId.isBlank()) {
            _uiState.update { it.copy(localMemberMarks = emptyList()) }
            return
        }
        val logs = trackingRepository.getCheckpointLogs(currentBookingId)
        val marks = logs.filter { it.syncStatus != SyncStatus.REJECTED.name && it.retryCount < 99 }.map { log ->
            LocalMemberMark(postId = log.postId, pendakiId = log.pendakiId, pending = log.syncStatus != SyncStatus.SENT.name, phase = log.phase)
        }
        _uiState.update { it.copy(localMemberMarks = marks) }
    }

    /**
     * Bangun timeline dua fase dari progres server + overlay antrean lokal.
     * Fase efektif tidak turun ke "naik" selama summit naik lokal masih pending.
     */
    private suspend fun rebuildTimeline() {
        if (currentBookingId.isBlank() || currentPendakiId.isBlank()) {
            _uiState.update { it.copy(postCheckpoints = emptyList()) }
            return
        }
        val localLogs = trackingRepository.getCheckpointLogs(currentBookingId)
            .filter { it.syncStatus != SyncStatus.REJECTED.name && it.retryCount < 99 }
        val baseHikers = serverHikers.ifEmpty {
            val visits = TrackingVisitUiMapper.fromPosts(loadedPosts)
            listOf(TrackingProgressV2Hiker(
                pendakiId = currentPendakiId, nama = "Saya", isLeader = _uiState.value.isKetua == true,
                trackingPhase = "naik", ascentProgress = PhaseProgress(0, loadedPosts.size, 0),
                descentProgress = PhaseProgress(0, (loadedPosts.size - 1).coerceAtLeast(0), 0),
                overallProgress = PhaseProgress(0, visits.size, 0), visits = visits
            ))
        }
        val hikers = baseHikers.map { TrackingVisitUiMapper.project(it, localLogs) }
        val my = hikers.firstOrNull { it.pendakiId == currentPendakiId }
        if (my == null) {
            _uiState.update { it.copy(postCheckpoints = emptyList(), progressHikers = hikers) }
            return
        }
        val pendingKeys = localLogs.filter {
            it.pendakiId == currentPendakiId && it.syncStatus != SyncStatus.SENT.name
        }.map { "${it.phase}:${it.postId}" }.toSet()
        val ui = TrackingVisitUiMapper.toCheckpointUi(my.visits, my.nextVisitKey, hikers, pendingKeys)
        _uiState.update {
            it.copy(
                postCheckpoints = ui, progressHikers = hikers,
                trackingPhase = my.trackingPhase, nextVisitKey = my.nextVisitKey,
                progressText = "Naik ${my.ascentProgress.completed}/${my.ascentProgress.total} · " +
                    "Turun ${my.descentProgress.completed}/${my.descentProgress.total} · " +
                    "Total ${my.overallProgress.completed}/${my.overallProgress.total} (${my.overallProgress.percentage}%)"
            )
        }
    }
    private fun pesanKonflik(error: Throwable): String {
        if (error is CheckpointApiException) {
            return when (error.code) {
                "DESCENT_REQUIRES_SUMMIT" -> "Check-in turun membutuhkan check-in puncak terlebih dahulu."
                "CHECKPOINT_PHASE_CONFLICT" -> "Fase atau waktu check-in tidak cocok. Muat ulang progress."
                "SUMMIT_HAS_DESCENT_CHECKINS" -> "Puncak tidak dapat dibatalkan karena sudah ada check-in turun."
                "CLIENT_EVENT_CONFLICT" -> "Konflik kejadian; periksa sinkronisasi."
                "ROUTE_CONFIGURATION_INVALID" -> "Jalur belum dikonfigurasi untuk fitur turun."
                else -> error.message ?: "Check-in gagal"
            }
        }
        return error.message ?: "Check-in gagal"
    }

    private suspend fun shouldQueueCheckIn(error: Throwable, phase: String, target: String, postId: Int): Boolean {
        if (adalahMasalahJaringan(error)) return true
        if (error !is CheckpointApiException) return false
        val logs = trackingRepository.getCheckpointLogs(currentBookingId)
        fun waiting(log: CheckpointLogEntity) = log.syncStatus != SyncStatus.SENT.name &&
            log.syncStatus != SyncStatus.REJECTED.name && log.retryCount < 99
        return (error.code == "DESCENT_REQUIRES_SUMMIT" && phase == "turun" && logs.any {
            it.pendakiId == target && it.isSummit && it.phase == "naik" && waiting(it)
        }) || (error.httpStatus == 403 && target != currentPendakiId && logs.any {
            it.pendakiId == currentPendakiId && it.postId == postId && it.phase == phase && waiting(it)
        })
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
        const val QR_PREFIX = "TNKS_CHECKPOINT:"
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
