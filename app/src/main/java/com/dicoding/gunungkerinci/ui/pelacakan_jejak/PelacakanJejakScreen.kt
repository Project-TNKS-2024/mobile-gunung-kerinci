package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dicoding.gunungkerinci.R

private sealed interface JejakScreenState {
    data object Ticket : JejakScreenState
    data object Timeline : JejakScreenState
    data object LocationDetail : JejakScreenState
}

@Composable
internal fun PelacakanJejakScreen(
    uiState: PelacakanJejakUiState = PelacakanJejakUiState(),
    onRetryLoadPosts: () -> Unit = {},
    onScanQrClick: () -> Unit = {},
    onGpsCheckInClick: () -> Unit = {},
    onManualCheckInConfirm: (postId: Int) -> Unit = {},
    onDismissGpsDialog: () -> Unit = {},
    onLocationDetailOpened: () -> Unit = {},
    /** Ketua tim menandai/membatalkan kehadiran anggota pada pos yang sedang dibuka. */
    onMemberToggle: (pendakiId: String, checked: Boolean, postId: Int?) -> Unit = { _, _, _ -> }
) {
    var screenState by remember { mutableStateOf<JejakScreenState>(JejakScreenState.Ticket) }
    var selectedCheckpoint by remember { mutableStateOf<JejakCheckpointUi?>(null) }
    var showQrSheet by remember { mutableStateOf(false) }

    // Auto-deteksi radius saat layar detail pos dibuka — pendaki langsung tahu
    // apakah dirinya boleh check-in tanpa harus menekan tombol cek dulu.
    LaunchedEffect(screenState) {
        if (screenState is JejakScreenState.LocationDetail) {
            onLocationDetailOpened()
        }
    }

    // Dialog konfirmasi GPS check-in — HANYA muncul saat backend mendeteksi
    // within_radius = true. Di luar radius, dialog ini tidak pernah tampil,
    // sehingga tidak ada jalan untuk check-in dari luar radius.
    val nearbyPost = uiState.nearbyPost
    if (nearbyPost != null) {
        AlertDialog(
            onDismissRequest = onDismissGpsDialog,
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
                    colors = ButtonDefaults.buttonColors(containerColor = JejakBlue)
                ) {
                    Text("Check-in", color = JejakWhite)
                }
            },
            dismissButton = {
                Button(
                    onClick = onDismissGpsDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = JejakBorder)
                ) {
                    Text("Batal", color = JejakTextPrimary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JejakWhite)
    ) {
        when (screenState) {
            JejakScreenState.Ticket -> PelacakanJejakTicketContent(
                uiState = uiState,
                onCheckPointClick = { screenState = JejakScreenState.Timeline }
            )

            JejakScreenState.Timeline -> PelacakanJejakTimelineContent(
                uiState = uiState,
                onBack = { screenState = JejakScreenState.Ticket },
                onCheckpointClick = { checkpoint ->
                    selectedCheckpoint = checkpoint
                    screenState = JejakScreenState.LocationDetail
                },
                onRetryLoadPosts = onRetryLoadPosts
            )

            JejakScreenState.LocationDetail -> PelacakanJejakLocationDetailScreen(
                detail = LocationDetailUiMapper.fromCheckpoint(
                    selectedCheckpoint,
                    uiState.loadedPosts
                ),
                members = TrackingProgressUiMapper.toMembers(
                    uiState.progressHikers,
                    selectedCheckpoint?.postId
                ),
                gpsGateState = uiState.gpsGateState,
                isCheckingGps = uiState.isCheckingGps,
                isMarkingMember = uiState.isMarkingMember,
                memberMarkMessage = uiState.memberMarkMessage,
                memberMarkErrorMessage = uiState.memberMarkErrorMessage,
                onBack = { screenState = JejakScreenState.Timeline },
                onScanQrClick = onScanQrClick,
                onGpsCheckInClick = onGpsCheckInClick,
                onMemberToggle = { pendakiId, checked ->
                    onMemberToggle(pendakiId, checked, selectedCheckpoint?.postId)
                }
            )
        }

//        if (showQrSheet) {
//            PelacakanJejakQrScanSheet(onDismiss = { showQrSheet = false })
//        }
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
    onCheckPointClick: () -> Unit = {}
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
        item { SectionTitle("Tiket Aktif") }
        item {
            if (activeTicket != null) {
                ActiveTicketCard(
                    ticket = activeTicket,
                    onCheckPointClick = onCheckPointClick,
                    badgeText = activeTicket.status,
                    checkPointEnabled = activeTicket.statusBooking == 6
                )
            } else {
                EmptyTicketCard(
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
                FinishedTicketCard(ticket = ticket)
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
private fun FinishedTicketCard(ticket: JejakTicketUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
    onRetryLoadPosts: () -> Unit = {}
) {
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
        if (uiState.progressText != null) {
            item { TimelineInfoCard(text = uiState.progressText) }
        } else if (uiState.isLoadingProgress) {
            item { TimelineInfoCard(text = "Memuat progress pendakian...") }
        } else if (uiState.progressErrorMessage != null) {
            item { TimelineInfoCard(text = uiState.progressErrorMessage) }
        }
        val periode = uiState.periodeLabel
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
            item { DaySelector(uiState.days) }
        }

        when {
            uiState.noActiveHikeMessage != null -> {
                item { TimelineInfoCard(text = uiState.noActiveHikeMessage) }
            }
            uiState.isLoadingPosts -> {
                item { TimelineInfoCard(text = "Memuat data post...") }
            }
            uiState.postErrorMessage != null -> {
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
                itemsIndexed(uiState.postCheckpoints) { index, item ->
                    TimelineRow(
                        item = item,
                        isFirst = index == 0,
                        isLast = index == uiState.postCheckpoints.lastIndex,
                        showCurrentBar = item.state == CheckpointState.Current && index > 0,
                        onClick = { onCheckpointClick(item) }
                    )
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
    val jumlahAnggota = uiState.progressHikers.size
    val hariAktif = uiState.days.firstOrNull { it.selected } ?: uiState.days.firstOrNull()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = JejakBlue
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.activeTicket?.status ?: "Pendakian Aktif",
                    color = JejakWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                hariAktif?.let { hari ->
                    Surface(shape = RoundedCornerShape(8.dp), color = JejakYellow) {
                        Text(
                            text = "${hari.title}!",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = JejakTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SummaryStat("Anggota", if (jumlahAnggota > 0) "$jumlahAnggota Orang" else "-")
                SummaryStat("Gate", uiState.activeTicket?.let { gateNameFrom(it) } ?: "-")
                SummaryStat("Tanggal", hariAktif?.date ?: "-")
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column {
        Text(label, color = JejakWhite.copy(alpha = 0.75f), fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = JejakWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Nama gate tidak disimpan di JejakTicketUi — pakai label masuk sebagai pengganti. */
private fun gateNameFrom(ticket: JejakTicketUi): String = ticket.masukDate

@Composable
private fun DaySelector(
    days: List<JejakDayUi>,
    onDaySelected: (Int) -> Unit = {}
) {
    if (days.isEmpty()) return
    var selectedIndex by remember(days) {
        mutableStateOf(days.indexOfFirst { it.selected }.coerceAtLeast(0))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        days.forEachIndexed { index, hari ->
            val isSelected = index == selectedIndex
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        selectedIndex = index
                        onDaySelected(index)
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) JejakBlue else JejakWhite,
                border = BorderStroke(1.dp, if (isSelected) JejakBlue else JejakBorder)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = hari.title,
                        color = if (isSelected) JejakWhite else JejakTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = hari.date,
                        color = if (isSelected) JejakWhite.copy(alpha = 0.85f) else JejakTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(
    item: JejakCheckpointUi,
    isFirst: Boolean,
    isLast: Boolean,
    showCurrentBar: Boolean,
    onClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showCurrentBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = JejakTextStrong
                ) {
                    Text(
                        text = "10.44",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = JejakWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(JejakTextPrimary)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .width(54.dp)
                    .padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item.times.forEach { t ->
                    Text(
                        text = t,
                        fontSize = 13.sp,
                        color = JejakBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            CheckpointCard(
                item = item,
                modifier = Modifier.weight(1f),
                onClick = onClick
            )
        }
    }
}

@Composable
private fun CheckpointCard(
    item: JejakCheckpointUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val borderColor = when (item.state) {
        CheckpointState.Completed -> JejakBorder
        CheckpointState.Current -> JejakBlue
        CheckpointState.Upcoming -> JejakBorder
    }
    val bg = when (item.state) {
        CheckpointState.Upcoming -> JejakBackground
        else -> JejakWhite
    }
    val titleColor = when (item.state) {
        CheckpointState.Upcoming -> JejakTextSecondary
        CheckpointState.Current -> JejakBlue
        CheckpointState.Completed -> JejakTextPrimary
    }
    val descColor = JejakTextSecondary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
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
            // Satu pembungkus vertikal (sesuai Figma):
            //   1. nama pos
            //   2. "checkpoint pada {jam} wib"  (fallback: deskripsi metode check-in)
            //   3. avatar anggota bertumpuk di ujung kanan
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = titleColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.checkedAtLabel ?: item.description,
                    color = descColor,
                    fontSize = 11.sp
                )
                if (item.memberNames.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    // Mengikuti alur kolom (rata kiri, di bawah baris waktu).
                    // Kalau Figma minta rata kanan: bungkus dengan
                    // Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End).
                    MemberAvatarStack(item.memberNames)
                }
            }
        }
    }
}

@Composable
private fun StatusDot(state: CheckpointState) {
    val (bg, glyph, glyphColor) = when (state) {
        CheckpointState.Completed -> Triple(JejakGreen, "✓", JejakWhite)
        CheckpointState.Current -> Triple(JejakBlue, "✓", JejakWhite)
        CheckpointState.Upcoming -> Triple(JejakGreyMid, "", JejakWhite)
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

/** Warna avatar bergiliran (seperti Figma: merah–oranye–biru). */
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

/** "Satria Bagus" → "SB"; nama satu kata → huruf pertama. */
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
