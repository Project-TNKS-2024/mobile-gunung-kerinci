package com.dicoding.gunungkerinci.ui.panic_button

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.model.SosCallOption

private val PrimaryBlue = Color(0xFF0169BF)
private val DangerRed = Color(0xFFCF372B)
private val SoftBlue = Color(0xFFEAF2FF)

/**
 * Bottom sheet "Bantuan Darurat" (dibuka dari tab SOS).
 * Kontak panggilan diambil dari GET /api/sos/call-options (tanpa nomor hardcode).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SosBottomSheet(
    uiState: SosUiState,
    onDismiss: () -> Unit,
    onKirimPesan: () -> Unit,
    onLaporan: () -> Unit,
    onSosTrigger: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Bantuan Darurat", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)

            SosMenuItem(
                iconRes = R.drawable.ic_messaging,
                title = "Kirim Pesan",
                subtitle = "Kirim pesan bantuan pada petugas",
                onClick = onKirimPesan
            )

            CallContactsSection(
                contacts = uiState.callOptions,
                onOpenLink = onOpenLink
            )

            SosMenuItem(
                iconRes = R.drawable.ic_disaster,
                title = "Laporkan Bencana",
                subtitle = "Laporkan potensi bencana kepada petugas",
                onClick = onLaporan
            )

            Text(
                text = "ⓘ  Hanya digunakan saat tidak memungkinkan melakukan panggilan atau mengirim pesan",
                fontSize = 12.sp,
                color = Color(0xFF434343),
                lineHeight = 16.sp
            )

            SosMenuItem(
                iconRes = R.drawable.ic_siren,
                title = "SOS (Keadaan Darurat)",
                subtitle = "Sistem akan mengirim sinyal SOS dan lokasi terakhir anda pada petugas.",
                sosItem = true,
                onClick = onSosTrigger
            )
        }
    }
}

@Composable
private fun CallContactsSection(
    contacts: List<SosCallOption>,
    onOpenLink: (String) -> Unit
) {
    // Hanya kontak pertama yang dipakai; kartu langsung menelepon saat diklik.
    val telLink = contacts.firstOrNull()?.telLink

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(enabled = telLink != null) { telLink?.let(onOpenLink) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.ic_emergency_call),
                contentDescription = null,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.width(22.dp))
            Column {
                Text("Melakukan Panggilan", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                Text(
                    "Hubungkan dengan petugas penjaga",
                    fontSize = 12.sp,
                    color = Color(0xFF434343)
                )
            }
        }
    }
}

@Composable
private fun SosMenuItem(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    sosItem: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
            .background(if (sosItem) DangerRed else Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.width(22.dp))
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (sosItem) Color.White else Color(0xFF1E1E1E)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (sosItem) Color.White else Color(0xFF434343),
                lineHeight = 17.sp
            )
        }
    }
}
