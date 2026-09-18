package com.dicoding.gunungkerinci.Homepage

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dicoding.gunungkerinci.R

/**
 * Grid menu "Berkas" di halaman Beranda — disamakan dengan repo `mobile-gunung-kerinci-vibe-coding`.
 * 6 item, ditata 4 kolom per baris → 4 di atas, 2 di bawah.
 */
private data class MenuItem(
    val label: String,
    @param:DrawableRes val iconRes: Int,
    val backgroundColor: Color,
    val onClick: () -> Unit
)

@Composable
fun BerkasMenuGrid(
    onPesanClick: () -> Unit,
    onVrJalurClick: () -> Unit,
    onSopClick: () -> Unit,
    onLaporanClick: () -> Unit,
    onSertifikatClick: () -> Unit,
    onPanduanClick: () -> Unit
) {
    val regular = FontFamily(Font(R.font.plus_jakarta_sans))

    val items = listOf(
        MenuItem("Pesan",            R.drawable.ic_messaging,       Color(0xFFDFE9F1), onPesanClick),
        MenuItem("VR Jalur",         R.drawable.ic_360_view,        Color(0xFFEFDFF1), onVrJalurClick),
        MenuItem("SOP",              R.drawable.ic_sop_img,         Color(0xFFFFF2D1), onSopClick),
        MenuItem("Laporan\nPendaki", R.drawable.ic_people_working,  Color(0xFFF1DFDF), onLaporanClick),
        MenuItem("Sertifikat",       R.drawable.ic_certificate,     Color(0xFFEEE0D8), onSertifikatClick),
        MenuItem("Panduan",          R.drawable.ic_user_manual,     Color(0xFFDFE9F1), onPanduanClick),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    MenuGridItem(
                        item = item,
                        fontFamily = regular,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MenuGridItem(item: MenuItem, fontFamily: FontFamily, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .size(60.dp)
                .background(color = item.backgroundColor, shape = RoundedCornerShape(16.dp))
                .clickable { item.onClick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.label,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.label,
            fontSize = 14.sp,
            fontFamily = fontFamily,
            color = Color(0xFF1E1E1E),
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}
