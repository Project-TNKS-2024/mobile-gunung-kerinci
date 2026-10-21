package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size

// All supplied maps are 1536 × 2752. Preserve the entire position annotation.
private const val MapAspectRatio = 1536f / 2752f

@Composable
internal fun TrailMapImage(url: String?, position: String) {
    if (url.isNullOrBlank()) {
        Text("Peta untuk lokasi ini belum tersedia.", color = JejakTextSecondary)
        return
    }
    var expanded by remember(url) { mutableStateOf(false) }
    Column {
        MapContent(
            url, position,
            Modifier.fillMaxWidth().aspectRatio(MapAspectRatio)
                .clickable(onClickLabel = "Perbesar peta") { expanded = true }
        )
        TextButton(onClick = { expanded = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Ketuk untuk memperbesar peta")
        }
    }
    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
                var scale by remember(url) { mutableFloatStateOf(1f) }
                var offset by remember(url) { mutableStateOf(Offset.Zero) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(position, color = Color.White, modifier = Modifier.weight(1f).padding(start = 16.dp))
                    TextButton(onClick = { scale = 1f; offset = Offset.Zero }) {
                        Text("Atur ulang", color = Color.White)
                    }
                    TextButton(onClick = { expanded = false }) {
                        Text("Tutup", color = Color.White)
                    }
                }
                BoxWithConstraints(
                    Modifier.fillMaxWidth().weight(1f).clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val viewportWidth = with(density) { maxWidth.toPx() }
                    val viewportHeight = with(density) { maxHeight.toPx() }
                    val imageWidth = minOf(viewportWidth, viewportHeight * MapAspectRatio)
                    val imageHeight = imageWidth / MapAspectRatio
                    MapContent(
                        url, position,
                        Modifier.fillMaxSize().pointerInput(url, viewportWidth, viewportHeight) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                val focus = centroid - Offset(viewportWidth / 2, viewportHeight / 2)
                                val proposed = (offset - focus) * (newScale / scale) + focus + pan
                                val maxX = ((imageWidth * newScale - viewportWidth) / 2).coerceAtLeast(0f)
                                val maxY = ((imageHeight * newScale - viewportHeight) / 2).coerceAtLeast(0f)
                                offset = Offset(proposed.x.coerceIn(-maxX, maxX), proposed.y.coerceIn(-maxY, maxY))
                                scale = newScale
                            }
                        }.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                    )
                }
                Text(
                    "Cubit untuk zoom · Geser untuk melihat detail",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun MapContent(url: String, position: String, modifier: Modifier) {
    var attempt by remember(url) { mutableIntStateOf(0) }
    val context = LocalContext.current
    val request = remember(url, attempt, context) {
        ImageRequest.Builder(context).data(url).size(Size.ORIGINAL).build()
    }
    key(attempt) {
        SubcomposeAsyncImage(
            model = request,
            contentDescription = "Peta posisi saat ini: $position",
            contentScale = ContentScale.Fit,
            modifier = modifier,
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = JejakBlue)
                }
            },
            error = {
                Column(
                    Modifier.fillMaxSize().background(JejakWhite),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Peta belum berhasil dimuat.", color = JejakTextSecondary)
                    TextButton(onClick = { attempt++ }) { Text("Coba lagi") }
                }
            }
        )
    }
}
