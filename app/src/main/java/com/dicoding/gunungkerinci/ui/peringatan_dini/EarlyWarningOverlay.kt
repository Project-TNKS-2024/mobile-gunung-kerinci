package com.dicoding.gunungkerinci.ui.peringatan_dini

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dicoding.gunungkerinci.R

/**
 * Overlay peringatan dini bahaya (Compose island di HomeFragment):
 * - bubble siren muncul selama ada peringatan aktif;
 * - popup detail muncul saat [EarlyWarningUiState.popupVisible];
 * - tombol X hanya menutup popup lokal, bubble tetap ada.
 */
@Composable
internal fun EarlyWarningOverlay(
    uiState: EarlyWarningUiState,
    onBubbleClick: () -> Unit,
    onClosePopup: () -> Unit
) {
    val warning = uiState.activeWarning

    Box(modifier = Modifier.fillMaxSize()) {
        if (warning != null) {
            EarlyWarningBubble(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp, bottom = 350.dp),
                onClick = onBubbleClick
            )
        }

        if (warning != null && uiState.popupVisible) {
            EarlyWarningDialog(
                warning = warning,
                onDismiss = onClosePopup
            )
        }
    }
}

@Composable
private fun EarlyWarningBubble(
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
                    colors = listOf(
                        Color(0xFFCF372B),
                        Color(0xFF691C16)
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
private fun EarlyWarningDialog(
    warning: EmergencyWarning,
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
                    ) { /* absorb klik di dalam card */ }
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Waktu terbit dari backend (created_at ISO-8601 → "yyyy-MM-dd HH:mm").
                    Text(
                        text = "Diterbitkan: " +
                            warning.createdAt.take(16).replace("T", " "),
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E8E)
                    )
                }

                // Icon melayang di atas card
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
