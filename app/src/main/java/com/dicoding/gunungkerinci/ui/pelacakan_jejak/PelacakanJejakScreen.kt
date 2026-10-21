package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.dicoding.gunungkerinci.ui.components.ProcessIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dicoding.gunungkerinci.R

private enum class JejakScreenState { Ticket, Timeline, LocationDetail }

@Composable
internal fun PelacakanJejakScreen(
    uiState: PelacakanJejakUiState = PelacakanJejakUiState(),
    onOpenTicket: (String) -> Unit = {},
    onRetryLoadPosts: () -> Unit = {},
    onDaySelected: (Int) -> Unit = {},
    onScanQrClick: () -> Unit = {},
    onGpsCheckInClick: () -> Unit = {},
    onManualCheckInConfirm: (postId: Int) -> Unit = {},
    onDismissGpsDialog: () -> Unit = {},
    onPrepareCheckIn: (visitKey: String, phase: String, postId: Int) -> Unit = { _, _, _ -> },
    onCheckInResultHandled: () -> Unit = {},
    /** Ketua tim menandai/membatalkan kehadiran anggota pada pos + fase yang sedang dibuka. */
    onMemberToggle: (pendakiId: String, checked: Boolean, postId: Int?, phase: String?) -> Unit = { _, _, _, _ -> },
    onMemberNoteSave: (String, String, Int?, String?) -> Unit = { _, _, _, _ -> }
) {
    var screenState by rememberSaveable { mutableStateOf(JejakScreenState.Ticket) }
    var selectedVisitKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCheckpoint = uiState.postCheckpoints.firstOrNull { it.visitKey == selectedVisitKey }
    var showCheckInMethods by rememberSaveable { mutableStateOf(false) }
    var checkingWithRadius by rememberSaveable { mutableStateOf(false) }

    // Tombol back HP mengikuti alur layar Jejak (detail pos â†’ timeline â†’ tiket), bukan
    // langsung melompat ke Beranda lewat navigasi tab bawah.
    BackHandler(enabled = screenState != JejakScreenState.Ticket) {
        checkingWithRadius = false
        showCheckInMethods = false
        onDismissGpsDialog()
        screenState = when (screenState) {
            JejakScreenState.LocationDetail -> JejakScreenState.Timeline
            else -> JejakScreenState.Ticket
        }
    }

    LaunchedEffect(uiState.checkedInVisitKey) {
        val checkedVisitKey = uiState.checkedInVisitKey ?: return@LaunchedEffect
        if (checkedVisitKey == selectedVisitKey) {
            checkingWithRadius = false
            showCheckInMethods = false
            screenState = JejakScreenState.LocationDetail
        }
        onCheckInResultHandled()
    }

    if (showCheckInMethods && selectedCheckpoint != null) {
        AlertDialog(
            onDismissRequest = { showCheckInMethods = false },
            containerColor = JejakWhite,
            titleContentColor = JejakTextPrimary,
            textContentColor = JejakTextBody,
            title = { Text(selectedCheckpoint.name) },
            text = { Text("Pilih metode check-in untuk pos ini.") },
            confirmButton = {
                Button(
                    onClick = {
                        showCheckInMethods = false
                        onScanQrClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JejakBlue)
                ) { Text("Scan QR", color = JejakWhite) }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showCheckInMethods = false
                        checkingWithRadius = true
                        onGpsCheckInClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JejakBlue)
                ) { Text("Radius", color = JejakWhite) }
            }
        )
    }

    if ((checkingWithRadius && uiState.isCheckingGps) || uiState.isCheckingIn) {
        AlertDialog(
            onDismissRequest = {
                checkingWithRadius = false
                onDismissGpsDialog()
            },
            containerColor = JejakWhite,
            title = { Text(if (uiState.isCheckingIn) "Mencatat Check-in" else "Memeriksa Radius", color = JejakTextPrimary) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = JejakBlue)
                    Spacer(Modifier.width(12.dp))
                    Text(if (uiState.isCheckingIn) "Mencatat kehadiran di ${selectedCheckpoint?.name.orEmpty()}."
                        else "Memeriksa lokasi untuk ${selectedCheckpoint?.name.orEmpty()}.")
                }
            },
            confirmButton = {}
        )
    }

    // Dialog konfirmasi GPS check-in \u2014 HANYA muncul saat backend mendeteksi
    // within_radius = true. Di luar radius, dialog ini tidak pernah tampil,
    // sehingga tidak ada jalan untuk check-in dari luar radius.
    val nearbyPost = uiState.nearbyPost
    if (checkingWithRadius && nearbyPost != null) {
        // GPS bisa mendeteksi pos lain daripada pos yang sedang dibuka (mis. pendaki masih
        // di Shelter 1 tetapi membuka layar Pos 2). Check-in ke pos lain itu tidak ditawarkan.
        val posDibuka = selectedCheckpoint
        val posSesuai = posDibuka?.postId == nearbyPost.id
        // Warna dialog disetel eksplisit: layar ini tidak dibungkus MaterialTheme, jadi
        // tanpa itu wadahnya ikut tema bawaan (abu-abu), bukan putih.
        if (posSesuai) {
            AlertDialog(
                onDismissRequest = { checkingWithRadius = false; onDismissGpsDialog() },
                containerColor = JejakWhite,
                titleContentColor = JejakTextPrimary,
                textContentColor = JejakTextBody,
                title = { Text("Check-in GPS") },
                text = {
                    Text(
                        "Kamu berada dalam radius pos \"${nearbyPost.nama}\" " +
                            "(${nearbyPost.distanceMeters.toInt()} m dari ${nearbyPost.radiusMeter} m radius).\n" +
                            "Konfirmasi check-in sekarang?"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onManualCheckInConfirm(nearbyPost.id) },
                        enabled = !uiState.isManualCheckingIn,
                        modifier = Modifier.width(160.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JejakBlue)
                    ) {
                        if (uiState.isManualCheckingIn) ProcessIndicator("Mencatat...") else Text("Check-in", color = JejakWhite)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { checkingWithRadius = false; onDismissGpsDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = JejakBorder)
                    ) {
                        Text("Batal", color = JejakTextPrimary)
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { checkingWithRadius = false; onDismissGpsDialog() },
                containerColor = JejakWhite,
                titleContentColor = JejakTextPrimary,
                textContentColor = JejakTextBody,
                title = { Text("Bukan Pos Ini") },
                text = {
                    Text(
                        "GPS mendeteksi kamu di pos \"${nearbyPost.nama}\" " +
                            "(${nearbyPost.distanceMeters.toInt()} m), bukan di " +
                            "\"${posDibuka?.name ?: "pos yang sedang dibuka"}\".\n" +
                            "Check-in hanya bisa di pos tempat kamu berada sekarang."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { checkingWithRadius = false; onDismissGpsDialog() },
                        colors = ButtonDefaults.buttonColors(containerColor = JejakBlue)
                    ) {
                        Text("Mengerti", color = JejakWhite)
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JejakWhite)
    ) {
        when (screenState) {
            JejakScreenState.Ticket -> PelacakanJejakTicketContent(
                uiState = uiState,
                onOpenTicket = onOpenTicket,
                onCheckPointClick = { screenState = JejakScreenState.Timeline }
            )

            JejakScreenState.Timeline -> PelacakanJejakTimelineContent(
                uiState = uiState,
                onBack = { screenState = JejakScreenState.Ticket },
                onCheckpointClick = { checkpoint ->
                    checkpoint.visitKey?.takeIf {
                        checkpoint.state != CheckpointState.Upcoming &&
                            !uiState.isCheckingIn && !uiState.isManualCheckingIn && !uiState.isCheckingGps
                    }?.let { visitKey ->
                        val postId = checkpoint.postId
                        val phase = checkpoint.phase
                        if (postId != null && phase != null) {
                            selectedVisitKey = visitKey
                            checkingWithRadius = false
                            onPrepareCheckIn(visitKey, phase, postId)
                            if (checkpoint.state == CheckpointState.Completed || phase != uiState.trackingPhase) {
                                showCheckInMethods = false
                                screenState = JejakScreenState.LocationDetail
                            } else {
                                showCheckInMethods = true
                            }
                        }
                    }
                },
                onRetryLoadPosts = onRetryLoadPosts,
                onDaySelected = onDaySelected
            )

            JejakScreenState.LocationDetail -> {
                // Server only unlocks posts where the leader has checked in (fase yang sama).
                val posDiBuka = selectedCheckpoint?.postId
                val faseDiBuka = selectedCheckpoint?.phase
                val bisaUbahKehadiran = uiState.isKetua == true && posDiBuka != null && faseDiBuka != null &&
                    TrackingProgressUiMapper.ketuaAdaDiPosIni(
                        posDiBuka, faseDiBuka, uiState.myPendakiId,
                        uiState.progressHikers, uiState.gpsGateState
                    )
                // Ada catatan yang belum terkirim di pos + fase ini? Itu masih boleh dibatalkan.
                val adaCatatanBelumTerkirim = uiState.localMemberMarks.any {
                    it.postId == posDiBuka && it.phase == faseDiBuka && it.pending
                }
                PelacakanJejakLocationDetailScreen(
                    detail = LocationDetailUiMapper.fromCheckpoint(
                        selectedCheckpoint,
                        uiState.loadedPosts
                    ),
                    members = TrackingProgressUiMapper.toMembers(
                        uiState.progressHikers,
                        posDiBuka,
                        faseDiBuka,
                        uiState.localMemberMarks,
                        uiState.memberNotes
                    ),
                    myPendakiId = uiState.myPendakiId,
                    bisaUbahKehadiran = bisaUbahKehadiran,
                    catatanKehadiran = when {
                        uiState.isKetua == false ->
                            "Hanya ketua tim yang dapat mengubah kehadiran anggota."
                        !bisaUbahKehadiran && adaCatatanBelumTerkirim ->
                            "Catatan yang belum terkirim masih bisa dibatalkan. Untuk menandai kehadiran baru, check-in dulu di pos ini."
                        else -> null
                    },
                    noteScopeKey = selectedVisitKey.orEmpty(),
                    savingNoteKey = uiState.savingMemberNoteKey,
                    noteErrorKey = uiState.memberNoteErrorKey,
                    noteError = uiState.memberNoteError,
                    isMarkingMember = uiState.isMarkingMember || uiState.savingMemberNoteKey != null,
                    processingMemberId = uiState.processingMemberId,
                    memberMarkMessage = uiState.memberMarkMessage,
                    memberMarkErrorMessage = uiState.memberMarkErrorMessage,
                    onBack = { screenState = JejakScreenState.Timeline },
                    onContinueHike = { screenState = JejakScreenState.Timeline },
                    onMemberToggle = { pendakiId, checked ->
                        onMemberToggle(pendakiId, checked, posDiBuka, faseDiBuka)
                    },
                    onMemberNoteSave = { pendakiId, note -> onMemberNoteSave(pendakiId, note, posDiBuka, faseDiBuka) }
                )
            }
        }
    }
}

// ---------- Shared header ----------

@Composable
private fun JejakTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(32.dp)
                .clickable(onClick = onBack),
            shape = CircleShape,
            color = JejakWhite,
            border = BorderStroke(1.dp, JejakBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_jejak_back),
                    contentDescription = "Kembali",
                    tint = JejakBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = "Jejak",
            color = JejakBlue,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ---------- Screen 1: Ticket ----------

@Composable
internal fun PelacakanJejakTicketContent(
    uiState: PelacakanJejakUiState = PelacakanJejakUiState(),
    onCheckPointClick: () -> Unit = {},
    onOpenTicket: (String) -> Unit = {}
) {
    val activeTicket = uiState.activeTicket
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 0.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { JejakTopBar(onBack = {}) }
        if (uiState.isLoadingTickets && (activeTicket != null || uiState.finishedTickets.isNotEmpty())) {
            item { ProcessIndicator("Memperbarui...") }
        }
        item { SectionTitle("Tiket Aktif") }
        item {
            when {
                uiState.isLoadingTickets && activeTicket == null && uiState.finishedTickets.isEmpty() -> LoadingTicketCard()
                activeTicket != null -> ActiveTicketCard(
                    ticket = activeTicket,
                    onCheckPointClick = onCheckPointClick,
                    badgeText = activeTicket.status,
                    checkPointEnabled = activeTicket.statusBooking == 6
                )
                else -> EmptyTicketCard(
                    text = uiState.noActiveHikeMessage
                        ?: "Belum ada pendakian aktif. Check-in tiket dulu untuk memakai Pelacakan Jejak."
                )
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
        item { SectionTitle("Tiket Selesai") }
        if (uiState.finishedTickets.isEmpty()) {
            item { EmptyTicketCard(text = "Belum ada tiket selesai.") }
        } else {
            items(uiState.finishedTickets) { ticket ->
                FinishedTicketCard(ticket = ticket, onClick = { ticket.rawBookingId?.let(onOpenTicket) })
            }
        }
    }
}

@Composable
private fun EmptyTicketCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = JejakWhite,
        border = BorderStroke(1.dp, JejakBorder)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = JejakTextSecondary
        )
    }
}

@Composable
private fun LoadingTicketCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = JejakWhite,
        border = BorderStroke(1.dp, JejakBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                color = JejakBlue
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = JejakTextPrimary
    )
}

@Composable
internal fun ActiveTicketCard(
    ticket: JejakTicketUi,
    onCheckPointClick: () -> Unit,
    badgeText: String = ticket.status,
    onBadgeClick: (() -> Unit)? = null,
    checkPointEnabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = JejakWhite,
        border = BorderStroke(1.dp, JejakBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = JejakBlue
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ticket.purchaseLabel,
                                    color = JejakWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = ticket.purchaseDate,
                                    color = JejakWhite,
                                    fontSize = 14.sp
                                )
                            }
                            Surface(
                                modifier = Modifier.then(
                                    if (onBadgeClick != null) Modifier.clickable { onBadgeClick() } else Modifier
                                ),
                                shape = RoundedCornerShape(8.dp),
                                color = JejakYellow
                            ) {
                                Text(
                                    text = badgeText,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = JejakTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        DashedDivider(color = JejakWhite.copy(alpha = 0.6f))
                        Spacer(Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                TicketInfoLine("Masuk", ticket.masukDate, light = true)
                                Spacer(Modifier.height(10.dp))
                                TicketInfoLine("Pendaki", ticket.pendakiCount, light = true)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                TicketInfoLine("Keluar", ticket.keluarDate, light = true)
                                Spacer(Modifier.height(10.dp))
                                TicketInfoLine("Total Pembayaran", ticket.totalPayment, light = true)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onCheckPointClick,
                    enabled = checkPointEnabled,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JejakBlue,
                        contentColor = JejakWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tabler_scan),
                        contentDescription = null,
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = ticket.primaryAction,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FinishedTicketCard(ticket: JejakTicketUi, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = JejakWhite,
        border = BorderStroke(1.dp, JejakBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.purchaseLabel,
                        color = JejakBlue,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = ticket.purchaseDate,
                        color = JejakTextPrimary,
                        fontSize = 13.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = JejakYellow
                ) {
                    Text(
                        text = ticket.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = JejakTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                DashedDivider(color = JejakBorder)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(JejakWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_jejak_hiker),
                        contentDescription = null,
                        tint = JejakBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    TicketInfoLine("Masuk", ticket.masukDate, light = false)
                    Spacer(Modifier.height(10.dp))
                    TicketInfoLine("Pendaki", ticket.pendakiCount, light = false)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    TicketInfoLine("Keluar", ticket.keluarDate, light = false)
                    Spacer(Modifier.height(10.dp))
                    TicketInfoLine("Total Pembayaran", ticket.totalPayment, light = false)
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(JejakBorder)
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detail",
                    color = JejakBlue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_jejak_chevron_right),
                    contentDescription = null,
                    tint = JejakBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TicketInfoLine(label: String, value: String, light: Boolean) {
    val labelColor = if (light) JejakWhite.copy(alpha = 0.85f) else JejakTextSecondary
    val valueColor = if (light) JejakWhite else JejakTextPrimary
    Column {
        Text(text = label, fontSize = 12.sp, color = labelColor)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun DashedDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
    }
}

// ---------- Screen 2: Timeline ----------

@Composable
internal fun PelacakanJejakTimelineContent(
    uiState: PelacakanJejakUiState,
    onBack: () -> Unit = {},
    onCheckpointClick: (JejakCheckpointUi) -> Unit = {},
    onRetryLoadPosts: () -> Unit = {},
    onDaySelected: (Int) -> Unit = {}
) {
    // Keep the entire route visible after check-in, even when actual/estimated
    // dates differ from the selected ticket day.
    val visibleCheckpoints = uiState.postCheckpoints
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 0.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { JejakTopBar(onBack = onBack) }
        item { HikingSummaryCard(uiState) }
        if (uiState.isLoadingProgress || (uiState.isLoadingPosts && uiState.postCheckpoints.isNotEmpty())) {
            item { ProcessIndicator(if (uiState.postCheckpoints.isEmpty()) "Memuat progres..." else "Memperbarui...") }
        }
        uiState.progressErrorMessage?.let { message ->
            item { TimelineInfoCard(text = message, actionText = "Coba Lagi", onActionClick = onRetryLoadPosts) }
        }
        if (uiState.postCheckpoints.isNotEmpty()) {
            uiState.postErrorMessage?.let { message ->
                item { TimelineInfoCard(text = message, actionText = "Coba Lagi", onActionClick = onRetryLoadPosts) }
            }
        }
        val periode = uiState.days.firstOrNull { it.selected }?.isoDate
            ?.takeIf { it.isNotBlank() }?.let(TicketUiMapper::formatTanggal) ?: uiState.periodeLabel
        if (!periode.isNullOrBlank()) {
            item {
                Text(
                    text = periode,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = JejakTextPrimary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        if (uiState.days.isNotEmpty()) {
            item { DaySelector(uiState.days, onDaySelected) }
        }

        when {
            uiState.noActiveHikeMessage != null -> {
                item { TimelineInfoCard(text = uiState.noActiveHikeMessage) }
            }
            uiState.isLoadingPosts && uiState.postCheckpoints.isEmpty() -> {
                item { ProcessIndicator("Memuat daftar pos...") }
            }
            uiState.postErrorMessage != null && uiState.postCheckpoints.isEmpty() -> {
                item {
                    TimelineInfoCard(
                        text = uiState.postErrorMessage,
                        actionText = "Coba Lagi",
                        onActionClick = onRetryLoadPosts
                    )
                }
            }
            uiState.postCheckpoints.isEmpty() -> {
                item { TimelineInfoCard(text = "Data post belum tersedia") }
            }
            else -> {
                val rows = buildTimelineRows(visibleCheckpoints)
                items(rows, key = { row ->
                    when (row) {
                        is TimelineRow.Header -> "header-${row.title}"
                        is TimelineRow.Checkpoint -> row.ui.visitKey ?: "${row.ui.postId}:${row.ui.phase}"
                    }
                }) { row ->
                    when (row) {
                        is TimelineRow.Header -> TimelineSectionHeader(row.title)
                        is TimelineRow.Checkpoint -> TimelineRow(
                            item = row.ui,
                            onClick = { onCheckpointClick(row.ui) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineInfoCard(
    text: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = JejakBackground,
        border = BorderStroke(1.dp, JejakBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                color = JejakTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onActionClick != null) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = JejakBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = actionText, color = JejakWhite, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun HikingSummaryCard(uiState: PelacakanJejakUiState) {
    val next = uiState.postCheckpoints.firstOrNull { it.state == CheckpointState.Current }
    val minutes = next?.travelMinutesFromPrevious?.takeIf { it >= 0 }
    val day = uiState.days.firstOrNull { it.selected }?.title?.let { "${it.trimEnd('!')}!" } ?: "Pendakian"
    val phase = if (uiState.trackingPhase == "turun") "Turun" else "Mendaki"
    val accent = Color(0xFFFBC433)
    val jakarta = FontFamily(Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium), Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0267C3)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.jejak_hiker_figma),
                contentDescription = null,
                modifier = Modifier.width(100.dp).height(88.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(day, color = accent, fontSize = 16.sp, lineHeight = 20.sp, fontFamily = jakarta, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = painterResource(R.drawable.jejak_hiking_status_figma), contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(phase, color = accent, fontSize = 12.sp, lineHeight = 20.sp, fontFamily = jakarta)
                    }
                }
                Text(
                    text = buildAnnotatedString {
                        if (minutes != null) {
                            append("Estimasi ")
                            withStyle(SpanStyle(color = accent)) { append("$minutes Menit") }
                            append(" menuju pemberhentian berikutnya.")
                        } else if (uiState.postCheckpoints.isNotEmpty() && uiState.postCheckpoints.all { it.state == CheckpointState.Completed }) {
                            append("Seluruh pos telah dikunjungi. Konfirmasi turun dilakukan oleh petugas.")
                        } else {
                            append("Estimasi menuju pemberhentian berikutnya belum tersedia.")
                        }
                    },
                    color = JejakWhite, fontSize = 12.sp, lineHeight = 18.sp, fontFamily = jakarta
                )
            }
        }
    }
}

/** Nama gate tidak disimpan di JejakTicketUi \u2014 pakai label masuk sebagai pengganti. */
private fun gateNameFrom(ticket: JejakTicketUi): String = ticket.masukDate

@Composable
private fun DaySelector(
    days: List<JejakDayUi>,
    onDaySelected: (Int) -> Unit = {}
) {
    if (days.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        itemsIndexed(days, key = { _, day -> day.isoDate.ifBlank { day.title } }) { index, day ->
            Column(
                modifier = Modifier.width(96.dp)
                    .selectable(selected = day.selected, role = Role.Tab, onClick = { onDaySelected(index) }),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = if (day.selected) JejakWhite else JejakBackground
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(day.title, color = if (day.selected) JejakBlue else JejakTextSubtle,
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(day.date, color = if (day.selected) JejakBlue else JejakTextSecondary,
                            fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (day.selected) JejakBlue else Color.Transparent))
            }
        }
    }
}

/** Baris timeline: header bagian Naik/Turun atau kartu kunjungan. */
private sealed interface TimelineRow {
    data class Header(val title: String) : TimelineRow
    data class Checkpoint(val ui: JejakCheckpointUi) : TimelineRow
}

private fun buildTimelineRows(checkpoints: List<JejakCheckpointUi>): List<TimelineRow> {
    val rows = mutableListOf<TimelineRow>()
    var lastPhase: String? = null
    for (c in checkpoints) {
        val phase = c.phase ?: continue
        if (phase != lastPhase) {
            rows += TimelineRow.Header(if (phase == "naik") "Naik" else "Turun")
            lastPhase = phase
        }
        rows += TimelineRow.Checkpoint(c)
    }
    return rows
}

@Composable
private fun TimelineSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = JejakTextPrimary,
        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

@Composable
private fun TimelineRow(
    item: JejakCheckpointUi,
    onClick: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = item.times.firstOrNull() ?: "\u2014",
            modifier = Modifier.width(54.dp).padding(top = 14.dp),
            fontSize = 16.sp,
            color = JejakBlue,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(8.dp))
        CheckpointCard(item = item, modifier = Modifier.weight(1f), onClick = onClick)
    }
}

@Composable
private fun CheckpointCard(
    item: JejakCheckpointUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val borderColor = JejakBorder
    val bg = JejakWhite
    val titleColor = when (item.state) {
        CheckpointState.Upcoming -> JejakGreyMid
        CheckpointState.Current -> JejakBlue
        CheckpointState.Completed -> JejakTextSubtle
    }
    val descColor = when (item.state) {
        CheckpointState.Current -> JejakBlue
        CheckpointState.Completed -> JejakTextSecondary
        else -> JejakGreyMid
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .let {
                if (onClick != null && item.state != CheckpointState.Upcoming) {
                    it.clickable(onClick = onClick)
                } else it
            },
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(state = item.state)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = titleColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (item.state == CheckpointState.Current) "Checkpoint selanjutnya"
                        else item.checkedAtLabel ?: item.description,
                    color = descColor,
                    fontSize = 14.sp
                )
                if (item.memberNames.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    MemberAvatarStack(item.memberNames)
                }
            }
        }
    }
}

@Composable
private fun StatusDot(state: CheckpointState) {
    val (bg, glyph, glyphColor) = when (state) {
        CheckpointState.Completed -> Triple(JejakDarkGreen, "\u2713", JejakWhite)
        CheckpointState.Current -> Triple(JejakBlue, "\u2713", JejakWhite)
        CheckpointState.Upcoming -> Triple(JejakGreyMid, "\u2713", JejakWhite)
    }
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (glyph.isNotEmpty()) {
            Text(
                text = glyph,
                color = glyphColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Warna avatar bergiliran (seperti Figma: merahâ€“oranyeâ€“biru). */
private val MemberAvatarColors = listOf(
    Color(0xFFE57373),
    Color(0xFFFFB74D),
    Color(0xFF64B5F6)
)

/**
 * Avatar anggota bertumpuk ala shadcn/ui: lingkaran 24.dp, saling menimpa 8.dp,
 * diberi cincin putih, isi inisial nama. Maksimal 3 tampil + penanda "+N".
 */
@Composable
private fun MemberAvatarStack(names: List<String>) {
    if (names.isEmpty()) return
    val tampil = names.take(3)
    val sisa = names.size - tampil.size
    Row(verticalAlignment = Alignment.CenterVertically) {
        tampil.forEachIndexed { i, nama ->
            Box(
                modifier = Modifier
                    .offset(x = if (i == 0) 0.dp else (-8).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MemberAvatarColors[i % MemberAvatarColors.size])
                    .border(2.dp, JejakWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = inisial(nama),
                    color = JejakWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (sisa > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-8).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(JejakGreyMid)
                    .border(2.dp, JejakWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$sisa",
                    color = JejakTextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** "Satria Bagus" â†’ "SB"; nama satu kata â†’ huruf pertama. */
private fun inisial(nama: String): String =
    nama.trim()
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun PreviewJejakTicketScreen() {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(JejakWhite)) {
        PelacakanJejakTicketContent(
            uiState = PelacakanJejakUiState(),
            onCheckPointClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun PreviewJejakTimelineScreen() {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(JejakWhite)) {
        PelacakanJejakTimelineContent(
            uiState = PelacakanJejakUiState()
        )
    }
}
