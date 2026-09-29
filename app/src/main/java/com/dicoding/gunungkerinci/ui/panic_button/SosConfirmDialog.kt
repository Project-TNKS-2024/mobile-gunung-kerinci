package com.dicoding.gunungkerinci.ui.panic_button

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

internal const val SOS_SEVERITY_LOW = "low"
internal const val SOS_SEVERITY_MEDIUM = "medium"
internal const val SOS_SEVERITY_HIGH = "high"

private val DangerRed = Color(0xFFCF372B)

private data class SeverityOption(val value: String, val label: String)

/**
 * Dialog konfirmasi kirim SOS + pilih tingkat bahaya.
 * Lokasi terakhir perangkat dikirim; gunakan hanya saat darurat.
 */
@Composable
internal fun SosConfirmDialog(
    isSending: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (severity: String) -> Unit
) {
    val options = listOf(
        SeverityOption(SOS_SEVERITY_LOW, "Rendah — butuh bantuan ringan"),
        SeverityOption(SOS_SEVERITY_MEDIUM, "Sedang — cedera / tersesat"),
        SeverityOption(SOS_SEVERITY_HIGH, "Tinggi — mengancam jiwa")
    )
    var selected by remember { mutableStateOf(SOS_SEVERITY_HIGH) }

    Dialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Kirim SOS Darurat?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Lokasi terakhir perangkat akan dikirim ke petugas TNKS. Gunakan hanya saat keadaan darurat.",
                    fontSize = 13.sp,
                    color = Color(0xFF667085)
                )
                Spacer(Modifier.height(14.dp))

                options.forEach { option ->
                    val checked = selected == option.value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selected = option.value }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(
                            modifier = Modifier
                                .size(18.dp)
                                .border(
                                    width = if (checked) 5.dp else 1.dp,
                                    color = if (checked) DangerRed else Color(0xFFB0B0B0),
                                    shape = CircleShape
                                )
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(option.label, fontSize = 14.sp, color = Color(0xFF1E1E1E))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSending) {
                        Text("Batal", color = Color(0xFF667085))
                    }
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = { onConfirm(selected) },
                        enabled = !isSending,
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                    ) {
                        Text(if (isSending) "Mengirim..." else "Kirim SOS")
                    }
                }
            }
        }
    }
}
