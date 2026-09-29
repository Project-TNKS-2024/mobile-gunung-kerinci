package com.dicoding.gunungkerinci.ui.panic_button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.model.SosChatMessage
import com.dicoding.gunungkerinci.network.DebugNetworkConfig
import java.io.File

private val EmergencyBlue = Color(0xFF1967D2)
private val SoftBlue = Color(0xFFEAF2FF)
private val ScreenBackground = Color(0xFFF6F8FC)
private val DangerRed = Color(0xFFE53935)

/** Layar chat SOS: daftar pesan + input teks/gambar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SosChatScreen(
    uiState: SosChatUiState,
    onBackClick: () -> Unit,
    onSendText: (String) -> Unit,
    onAttachImageClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    val totalBubble = uiState.messages.size + uiState.queuedMessages.size
    LaunchedEffect(totalBubble) {
        if (totalBubble > 0) listState.animateScrollToItem(totalBubble - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Pesan Darurat", fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("‹", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EmergencyBlue)
            )
        },
        containerColor = ScreenBackground,
        bottomBar = {
            MessageInputBar(
                value = input,
                isSending = uiState.isSending,
                onInputChange = { input = it },
                onSendClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        onSendText(text)
                        input = ""
                    }
                },
                onAttachImageClick = onAttachImageClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.messages.isEmpty() && uiState.queuedMessages.isEmpty() -> EmptyState(onRefreshClick)
                else -> {
                    uiState.errorMessage?.let { ErrorBanner(it) }
                    uiState.queuedInfo?.let { InfoBanner(it) }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            MessageBubble(message)
                        }
                        items(uiState.queuedMessages, key = { it.localId }) { queued ->
                            QueuedBubble(queued)
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = EmergencyBlue)
            Spacer(Modifier.height(12.dp))
            Text("Memuat chat darurat...", color = Color(0xFF344054))
        }
    }
}

@Composable
private fun EmptyState(onRefreshClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, tonalElevation = 2.dp) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Belum ada pesan", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF101828))
                Spacer(Modifier.height(8.dp))
                Text("Kirim pesan untuk menghubungi petugas.", color = Color(0xFF667085), fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRefreshClick, colors = ButtonDefaults.buttonColors(containerColor = EmergencyBlue)) {
                    Text("Muat Ulang")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SosChatMessage) {
    val isHiker = message.senderType.equals("hiker", ignoreCase = true)
    val isImage = message.type.equals("image", ignoreCase = true)

    // Pesan gambar tanpa bubble: langsung gambarnya saja.
    val bubbleModifier = if (isImage) {
        Modifier
    } else {
        Modifier
            .background(
                color = if (isHiker) EmergencyBlue else Color.White,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isHiker) 18.dp else 4.dp,
                    bottomEnd = if (isHiker) 4.dp else 18.dp
                )
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isHiker) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(bubbleModifier)
        ) {
            if (!isHiker) {
                Text(message.senderName ?: "Admin TNKS", color = EmergencyBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
            }
            if (isImage) {
                AsyncImage(
                    model = message.content.orEmpty().toAbsoluteApiUrl(),
                    contentDescription = "Lampiran gambar SOS",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(max = 252.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Text(
                    text = message.content.orEmpty(),
                    color = if (isHiker) Color.White else Color(0xFF101828),
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = message.createdAt.orEmpty().take(16).replace("T", " "),
                color = if (isImage) Color(0xFF98A2B3)
                else if (isHiker) Color.White.copy(alpha = 0.72f)
                else Color(0xFF98A2B3),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .background(DangerRed.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        color = DangerRed,
        fontSize = 13.sp
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun MessageInputBar(
    value: String,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachImageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            enabled = !isSending,
            placeholder = { Text("Tulis pesan anda", color = Color(0xFFB3B3B3), fontSize = 14.sp) },
            maxLines = 4,
            shape = CircleShape,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAttachImageClick, enabled = !isSending) {
                        Icon(
                            painter = painterResource(R.drawable.ic_gallery),
                            contentDescription = "Lampirkan gambar",
                            tint = EmergencyBlue
                        )
                    }
                    IconButton(onClick = onSendClick, enabled = !isSending) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = EmergencyBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_send),
                                contentDescription = "Kirim",
                                tint = EmergencyBlue
                            )
                        }
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFB3B3B3),
                unfocusedBorderColor = Color(0xFFB3B3B3),
                disabledBorderColor = Color(0xFFB3B3B3),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedPlaceholderColor = Color(0xFFB3B3B3),
                unfocusedPlaceholderColor = Color(0xFFB3B3B3),
                cursorColor = EmergencyBlue
            )
        )
    }
}

private fun String.toAbsoluteApiUrl(): String {
    val value = trim()
    if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("content://")) return value
    val baseUrl = DebugNetworkConfig.BASE_URL.trimEnd('/')
    return if (value.startsWith('/')) "$baseUrl$value" else "$baseUrl/$value"
}

/** Bubble pesan lokal yang menunggu dikirim (antrean offline, US-12). */
@Composable
private fun QueuedBubble(queued: QueuedChatUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = SoftBlue,
                    shape = RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = 4.dp, bottomEnd = 18.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (queued.isImage && !queued.imagePath.isNullOrBlank()) {
                AsyncImage(
                    model = File(queued.imagePath),
                    contentDescription = "Pesan gambar menunggu kirim",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .widthIn(max = 252.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Text(
                    text = queued.content.orEmpty(),
                    color = Color(0xFF101828),
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "⏳ Menunggu kirim",
                color = Color(0xFF98A2B3),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun InfoBanner(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftBlue, RoundedCornerShape(12.dp))
            .padding(12.dp),
        color = EmergencyBlue,
        fontSize = 13.sp
    )
    Spacer(Modifier.height(10.dp))
}
