package com.dicoding.gunungkerinci.ui.panic_button

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private val PrimaryBlue = Color(0xFF0169BF)
private val ErrorRed = Color(0xFFF84233)

/**
 * Form laporan potensi bencana (dalam bottom sheet).
 * Validasi memakai DisasterReportValidator; pengiriman lewat DisasterReportViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DisasterReportSheet(
    uiState: DisasterReportUiState,
    onDismiss: () -> Unit,
    onSubmit: (potensi: String, deskripsi: String, lokasi: String, lampiran: MultipartBody.Part?) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var potensi by remember { mutableStateOf("") }
    var deskripsi by remember { mutableStateOf("") }
    var lokasi by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    var lampiranUri by remember { mutableStateOf<Uri?>(null) }
    var lampiranName by remember { mutableStateOf("") }
    var lampiranMime by remember { mutableStateOf<String?>(null) }
    var lampiranSize by remember { mutableStateOf(0L) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        lampiranUri = uri
        lampiranName = uri?.let { context.getFileName(it) }.orEmpty()
        lampiranMime = uri?.let { context.contentResolver.getType(it) }
        lampiranSize = uri?.let { context.getFileSize(it) } ?: 0L
        error = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Laporkan Potensi Bencana", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
            Spacer(Modifier.height(16.dp))

            FormField("Potensi Bencana", "Contoh: Tanah Longsor", potensi) { potensi = it }
            Spacer(Modifier.height(8.dp))
            FormField("Deskripsi Lengkap", "Jelaskan apa yang terjadi", deskripsi, minLines = 3) { deskripsi = it }
            Spacer(Modifier.height(8.dp))
            FormField("Lokasi", "Deskripsikan lokasi dengan rinci", lokasi, minLines = 3) { lokasi = it }
            Spacer(Modifier.height(8.dp))

            LampiranField(name = lampiranName, onPick = { picker.launch("image/*") })

            val tampilError = error ?: uiState.errorMessage
            val tampilSukses = uiState.successMessage
            if (tampilError != null) {
                Spacer(Modifier.height(12.dp))
                Text(tampilError, fontSize = 13.sp, color = ErrorRed)
            }
            if (tampilSukses != null) {
                Spacer(Modifier.height(12.dp))
                Text(tampilSukses, fontSize = 13.sp, color = Color(0xFF1B8765))
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val validasi = DisasterReportValidator.error(potensi, deskripsi, lokasi, lampiranMime, lampiranSize)
                    if (validasi != null) {
                        error = validasi
                        return@Button
                    }
                    error = null
                    onSubmit(potensi, deskripsi, lokasi, lampiranUri?.toPart(context, lampiranName, lampiranMime))
                },
                enabled = !uiState.isSending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (uiState.isSending) "Mengirim..." else "Kirim", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    placeholder: String,
    value: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    Column {
        Row {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.width(4.dp))
            Text("*", fontSize = 16.sp, color = ErrorRed)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            minLines = minLines,
            placeholder = { Text(placeholder, color = Color(0xFFCCCCCC)) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                focusedPlaceholderColor = Color(0xFFCCCCCC),
                unfocusedPlaceholderColor = Color(0xFFCCCCCC)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun LampiranField(name: String, onPick: () -> Unit) {
    Column {
        Row {
            Text("Lampiran", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.width(4.dp))
            Text("*", fontSize = 16.sp, color = ErrorRed)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                "Pilih File",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue,
                modifier = Modifier.clickable { onPick() }
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = name.ifBlank { "Pilih Foto" },
                fontSize = 14.sp,
                color = if (name.isBlank()) Color(0xFFCCCCCC) else Color(0xFF1E1E1E)
            )
        }
    }
}

private fun android.content.Context.getFileName(uri: Uri): String {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx)
    }
    return uri.lastPathSegment ?: "lampiran_bencana.jpg"
}

private fun android.content.Context.getFileSize(uri: Uri): Long {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (idx >= 0 && cursor.moveToFirst()) return cursor.getLong(idx)
    }
    return 0L
}

private fun Uri.toPart(context: android.content.Context, name: String, mime: String?): MultipartBody.Part? {
    val bytes = runCatching { context.contentResolver.openInputStream(this)?.use { it.readBytes() } }.getOrNull()
        ?: return null
    val fileName = name.ifBlank { "lampiran_bencana.jpg" }
    val body = bytes.toRequestBody((mime ?: "image/jpeg").toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("lampiran", fileName, body)
}
