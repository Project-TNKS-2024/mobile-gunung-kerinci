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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dicoding.gunungkerinci.R

@Composable
internal fun PelacakanJejakLocationDetailScreen(
    detail: LocationDetailUi = LocationDetailUi.Default,
    members: List<JejakMemberUi> = emptyList(),
    gpsGateState: GpsGateState = GpsGateState.Unknown,
    isCheckingGps: Boolean = false,
    onBack: () -> Unit,
    onScanQrClick: () -> Unit,
    onGpsCheckInClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEAF6FF), JejakWhite, JejakWhite)
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        LocationTopBar(onBack)
        Spacer(Modifier.height(8.dp))
        WelcomeCheckpointCard(
            positionLabel = detail.positionLabel,
            postLabel = detail.postLabel,
            placeName = detail.placeName,
            statusText = detail.statusText
        )
        Spacer(Modifier.height(22.dp))
        SectionTitleLocal("Informasi Lokasi")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompactInfoCard(R.drawable.jejak_icon_distance, detail.distanceToNextLabel, detail.distanceToNextValue, Modifier.weight(1f))
            CompactInfoCard(R.drawable.jejak_icon_avg_time, "Estimasi rata-rata", detail.averageEstimateValue, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        MountainRouteIllustration()
        Spacer(Modifier.height(14.dp))
        LocationInfoGrid(
            altitudeText = detail.altitudeText,
            waterSourceStatus = detail.waterSourceStatus,
            campingStatus = detail.campingStatus,
            mountainStatus = detail.mountainStatus,
            statusText = detail.statusText,
            checkpointDescription = detail.checkpointDescription
        )
        Spacer(Modifier.height(24.dp))
        SectionTitleLocal("Cek Anggota")
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Cek kelengkapan anggota tim anda, yang\nsampai bersama pada titik ini.",
            color = JejakTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(14.dp))
        if (members.isEmpty()) {
            Text(
                text = "Belum ada data anggota untuk pendakian ini.",
                color = JejakTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        } else {
            members.forEachIndexed { index, member ->
                MemberCheckCard(
                    name = member.name,
                    avatarColor = avatarColorFor(member.name),
                    checked = member.checked,
                )
                if (index != members.lastIndex) {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onScanQrClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JejakBlue, contentColor = JejakWhite)
        ) {
            Icon(painterResource(R.drawable.ic_jejak_arrow_right), contentDescription = null, tint = JejakWhite, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Scan QR Check-in", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(10.dp))
        GpsCheckInButton(
            gateState = gpsGateState,
            isChecking = isCheckingGps,
            onClick = onGpsCheckInClick
        )
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Tombol check-in GPS dengan penegakan aturan keselamatan:
 * check-in hanya mungkin saat GPS hidup DAN pendaki berada di dalam radius pos.
 *
 * Aksi tombol selalu "deteksi posisi" (read-only). Absen sebenarnya baru terjadi
 * lewat dialog konfirmasi yang MUNCUL HANYA ketika within_radius = true — jadi di
 * luar radius pendaki tidak punya jalan untuk check-in.
 *
 * - [GpsGateState.Unknown]     → "Cek Posisi GPS"
 * - [GpsGateState.GpsOff]      → "Aktifkan GPS & Cek Ulang"
 * - [GpsGateState.LuarRadius]  → "Cek Ulang Posisi" (tidak bisa absen)
 * - [GpsGateState.DalamRadius] → "Check-in Pos Ini" (aktif, hijau)
 */
@Composable
private fun GpsCheckInButton(
    gateState: GpsGateState,
    isChecking: Boolean,
    onClick: () -> Unit
) {
    val isReady = gateState is GpsGateState.DalamRadius

    val label = when {
        isChecking -> "Memeriksa Posisi GPS..."
        isReady -> "Check-in Pos Ini"
        gateState is GpsGateState.LuarRadius -> "Cek Ulang Posisi"
        gateState is GpsGateState.GpsOff -> "Aktifkan GPS & Cek Ulang"
        else -> "Cek Posisi GPS"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onClick,
            enabled = !isChecking,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isReady) JejakGreen else JejakWhite,
                contentColor = if (isReady) JejakWhite else JejakBlue,
                disabledContainerColor = JejakBorder,
                disabledContentColor = JejakTextSecondary
            ),
            border = if (isReady) null else BorderStroke(1.dp, JejakBlue)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_jejak_qr),
                contentDescription = null,
                tint = if (isReady) JejakWhite else JejakBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        // Alasan terkunci — ditampilkan di bawah tombol supaya pendaki tahu kenapa tidak bisa absen.
        val reason = when (gateState) {
            is GpsGateState.GpsOff ->
                "GPS tidak aktif / izin lokasi ditolak. Check-in tidak bisa dilakukan sebelum GPS aktif."
            is GpsGateState.LuarRadius ->
                "Kamu ${gateState.distanceMeters.toInt()} m dari ${gateState.postName} " +
                        "(radius ${gateState.radiusMeter} m). Di luar radius, check-in tidak diizinkan."
            is GpsGateState.DalamRadius ->
                "Kamu berada ${gateState.distanceMeters.toInt()} m dari ${gateState.postName} — siap check-in."
            GpsGateState.Unknown -> null
        }
        if (reason != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = reason,
                color = when (gateState) {
                    is GpsGateState.DalamRadius -> JejakGreen
                    is GpsGateState.LuarRadius -> JejakYellowDark
                    else -> JejakTextSecondary
                },
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun LocationTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Jejak", color = JejakBlue, fontWeight = FontWeight.Bold, fontSize = 22.sp)
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
    }
}

@Composable
private fun WelcomeCheckpointCard(
    positionLabel: String = "POS 3",
    postLabel: String = "Pos 3:",
    placeName: String = "Pondok Panorama",
    statusText: String = "Posisi saat ini"
) {
    // Card sambutan: ilustrasi man_with_map + latar biru langit #559BD4, radius 8.
    // Teks 16.sp semua — "Selamat Datang!" #CCCCCC, "Saat ini anda berada di" #232323,
    // "POS n" #0169BF.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(JejakBlueSky)
            .padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.man_with_map),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 100.dp, height = 88.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Selamat Datang!",
                color = JejakGreyMid,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Saat ini anda berada di",
                color = JejakTextBody,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = positionLabel,
                color = JejakBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HikerIllustration() {
    Canvas(Modifier.size(116.dp)) {
        drawCircle(Color(0xFFD9EEFF), radius = size.minDimension * .43f, center = center)
        drawCircle(JejakYellow, radius = 9.dp.toPx(), center = Offset(size.width * .48f, size.height * .23f))
        drawRoundRect(JejakWhite.copy(.9f), Offset(size.width * .14f, size.height * .2f), Size(25.dp.toPx(), 9.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
        drawCircle(Color(0xFF6B4B38), radius = 10.dp.toPx(), center = Offset(size.width * .53f, size.height * .38f))
        drawRoundRect(JejakDarkBlue, Offset(size.width * .27f, size.height * .42f), Size(38.dp.toPx(), 45.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
        drawRoundRect(Color(0xFF324B61), Offset(size.width * .55f, size.height * .44f), Size(27.dp.toPx(), 38.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
        drawLine(Color(0xFF303030), Offset(size.width * .45f, size.height * .74f), Offset(size.width * .30f, size.height * .92f), 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Color(0xFF303030), Offset(size.width * .55f, size.height * .74f), Offset(size.width * .72f, size.height * .90f), 5.dp.toPx(), cap = StrokeCap.Round)
        drawRoundRect(JejakWhite, Offset(size.width * .67f, size.height * .48f), Size(25.dp.toPx(), 18.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
    }
}

@Composable
private fun CompactInfoCard(iconRes: Int, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.height(76.dp), shape = RoundedCornerShape(16.dp), color = JejakWhite, border = BorderStroke(1.dp, JejakBorder)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, color = JejakTextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
                Text(value, color = JejakTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MountainRouteIllustration() {
    // Asset langsung dari Figma node 1521:9127 supaya gunung dan rute identik dengan desain.
    Image(
        painter = painterResource(R.drawable.jejak_mountain_route),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}

@Composable
private fun LocationInfoGrid(
    altitudeText: String,
    waterSourceStatus: String,
    campingStatus: String,
    mountainStatus: String,
    statusText: String,
    checkpointDescription: String
) {
    // Audit 2026-09-16: label diperbaiki supaya cocok dengan nilainya.
    // CATATAN: ikon dua kotak dikembalikan ke versi semula atas permintaan Satria
    // (ic_temperature_16_filled & ic_sort_amount_up_alt) — jadi label baru + ikon lama.
    val items = listOf(
        Triple("Status Gunung", mountainStatus, R.drawable.ic_mount_fuji),
        Triple("Ketinggian", altitudeText, R.drawable.ic_mountain_location_top),
        Triple("Status Check-in", statusText, R.drawable.ic_temperature_16_filled),
        Triple("Sumber Mata Air", waterSourceStatus, R.drawable.ic_water),
        Triple("Checkpoint", checkpointDescription, R.drawable.ic_sort_amount_up_alt),
        Triple("Berkemah", campingStatus, R.drawable.ic_camping),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(3).forEach { baris ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                baris.forEach { (label, value, icon) ->
                    InfoGridItem(label, value, icon, Modifier.weight(1f))
                }
                if (baris.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoGridItem(label: String, value: String, iconRes: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(14.dp),
        color = JejakWhite,
        border = BorderStroke(1.dp, JejakBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = JejakBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(label, color = JejakTextSecondary, fontSize = 10.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = JejakTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun MemberCheckCard(name: String, avatarColor: Color, checked: Boolean) {
    var isChecked by remember { mutableStateOf(checked) }
    Surface(shape = RoundedCornerShape(18.dp), color = JejakWhite, border = BorderStroke(1.dp, JejakBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(avatarColor), contentAlignment = Alignment.Center) {
                    Text(name.first().toString(), color = JejakWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(name, color = JejakTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                MemberToggle(checked = isChecked, onCheckedChange = { isChecked = it })
            }
            if (!isChecked) {
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = JejakWhite, border = BorderStroke(1.dp, JejakBorder), modifier = Modifier.fillMaxWidth().height(42.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_jejak_plus), contentDescription = null, tint = JejakTextSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tambahkan Catatan", color = JejakTextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = JejakWhite,
            checkedTrackColor = JejakGreen,
            uncheckedThumbColor = JejakWhite,
            uncheckedTrackColor = Color(0xFFE14949),
            uncheckedBorderColor = Color.Transparent,
            checkedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun SectionTitleLocal(text: String) {
    Text(text, color = JejakTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}

/** Warna avatar ditentukan dari nama (deterministik) — bukan nilai hard-code per orang. */
private val AvatarPalette = listOf(
    Color(0xFFE85D74), Color(0xFF2F3A46), Color(0xFFB56B4D),
    Color(0xFF4A7A96), Color(0xFF7A5C9E), Color(0xFF3E7D5A)
)

private fun avatarColorFor(name: String): Color =
    AvatarPalette[((name.hashCode() % AvatarPalette.size) + AvatarPalette.size) % AvatarPalette.size]

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Composable
private fun PreviewLocationDetail() {
    PelacakanJejakLocationDetailScreen(
        onBack = {},
        onScanQrClick = {}
    )
}
