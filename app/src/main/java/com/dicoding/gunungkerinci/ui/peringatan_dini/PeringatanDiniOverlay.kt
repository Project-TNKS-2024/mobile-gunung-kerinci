package com.dicoding.gunungkerinci.ui.peringatan_dini

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.dicoding.gunungkerinci.ui.components.ProcessIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dicoding.gunungkerinci.R

@Composable
internal fun PeringatanDiniOverlay(
    uiState: PeringatanDiniUiState,
    onBubbleClick: () -> Unit,
    onClosePopup: () -> Unit,
    onUpdateLocation: () -> Unit,
    alarmRinging: Boolean = false,
    onStopAlarm: () -> Unit = {}
) {
    val warning = uiState.activeWarning

    Box(modifier = Modifier.fillMaxSize()) {
        if (warning != null) {
            PeringatanDiniBubble(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp, bottom = 350.dp),
                onClick = onBubbleClick
            )
        }

        if (warning == null && uiState.popupVisible) {
            AlertDialog(onDismissRequest = onClosePopup,
                title = { Text("Peringatan Dini") },
                text = {
                    when {
                        uiState.isLoadingWarnings -> ProcessIndicator("Memeriksa peringatan…")
                        uiState.errorMessage != null -> Text(uiState.errorMessage)
                        uiState.hasLoadedWarnings -> Text("Tidak ada peringatan aktif.")
                        else -> Text("Peringatan belum diperiksa.")
                    }
                },
                confirmButton = { TextButton(onClick = onClosePopup) { Text("Tutup") } })
        }
        if (warning != null && uiState.popupVisible) {
            PeringatanDiniDialog(
                warning = warning,
                isUpdatingLocation = uiState.isUpdatingLocation,
                isSendingLocation = uiState.isSendingLocation,
                isLoadingWarnings = uiState.isLoadingWarnings,
                lastLocationUpdate = uiState.lastLocationUpdate,
                locationUpdateMessage = uiState.locationUpdateMessage,
                lastUpdatedAt = uiState.lastUpdatedAt,
                isStale = uiState.isStale,
                alarmRinging = alarmRinging,
                onStopAlarm = onStopAlarm,
                onUpdateLocation = onUpdateLocation,
                onDismiss = onClosePopup
            )
        }
    }
}

@Composable
private fun PeringatanDiniBubble(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(100.dp)
            .border(2.dp, Color(0xFFFED9D6), CircleShape)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFFCF372B),
                        0.68f to Color(0xFFCF372B),
                        1f to Color(0xFF691C16)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_siren),
            contentDescription = "Peringatan Dini",
            modifier = Modifier.size(52.dp)
        )
    }
}

@Composable
private fun PeringatanDiniDialog(
    warning: PeringatanDini,
    isUpdatingLocation: Boolean,
    isSendingLocation: Boolean,
    isLoadingWarnings: Boolean,
    lastLocationUpdate: String?,
    locationUpdateMessage: String?,
    lastUpdatedAt: String?,
    isStale: Boolean,
    alarmRinging: Boolean,
    onStopAlarm: () -> Unit,
    onUpdateLocation: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {  }
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF1E1E1E),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 12.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onDismiss() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pemberitahuan Darurat",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFCF372B),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = warning.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1E1E),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = warning.description,
                        fontSize = 14.sp,
                        color = Color(0xFF444444),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    if (isLoadingWarnings) ProcessIndicator("Memperbarui…")
                    if (lastUpdatedAt != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isStale) {
                                "Data peringatan terakhir: $lastUpdatedAt (belum tersinkron)"
                            } else {
                                "Diperbarui $lastUpdatedAt"
                            },
                            fontSize = 12.sp,
                            color = if (isStale) Color(0xFFB26A00) else Color(0xFF8E8E8E),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Tombol update lokasi
                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !isUpdatingLocation
                            ) { onUpdateLocation() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_rotation),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = "Update Lokasi Terakhir di Sistem",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCF372B),
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (lastLocationUpdate != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Terakhir di update $lastLocationUpdate",
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E8E)
                        )
                    }

                    if (isUpdatingLocation) {
                        ProcessIndicator(if (isSendingLocation) "Mengirim lokasi…" else "Mencari lokasi…")
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (locationUpdateMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = locationUpdateMessage,
                            fontSize = 12.sp,
                            color = Color(0xFFE14949),
                            textAlign = TextAlign.Center
                        )
                    }

                    if (alarmRinging) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onStopAlarm,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFCF372B),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Hentikan alarm", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Image(
                    painter = painterResource(R.drawable.ic_siren),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}
