package com.dicoding.gunungkerinci.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dicoding.gunungkerinci.R

/**
 * Tab pada bottom bar custom — sama dengan repo `mobile-gunung-kerinci-vibe-coding`.
 * SOS tidak dimasukkan sebagai tujuan navigasi (belum ada fragment-nya di repo ini).
 */
enum class NavTab { BERANDA, TIKET, SOS, CHECK_POINT, AKUN }

private val PrimaryBlue = Color(0xFF0169BF)
private val BarGrey = Color(0xFF8E8E8E)

/**
 * Bottom bar custom (desain disamakan dengan repo vibe-coding).
 * Klik tab SOS memanggil [onSosClick] — pemanggil yang membuka SosBottomSheet.
 */
@Composable
fun CustomBottomBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    onSosClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        // Bar putih dengan sudut atas membulat 16dp (tanpa shadow → tidak tampak "melayang")
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(90.dp)
                .border(
                    width = 1.dp,
                    color = Color(0xFFE6E6E6),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .background(Color.White, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    tab = NavTab.BERANDA,
                    label = "Beranda",
                    iconRes = R.drawable.beranda,
                    iconFillRes = R.drawable.beranda_fill,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
                BottomNavItem(
                    tab = NavTab.TIKET,
                    label = "Tiket",
                    iconRes = R.drawable.ticket,
                    iconFillRes = R.drawable.ticket_fill,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
                SosBottomNavItem(
                    label = "SOS",
                    onClick = onSosClick
                )
                BottomNavItem(
                    tab = NavTab.CHECK_POINT,
                    label = "Check Point",
                    iconRes = R.drawable.ic_tabler_scan,
                    iconFillRes = R.drawable.ic_tabler_scan_fill,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
                BottomNavItem(
                    tab = NavTab.AKUN,
                    label = "Akun",
                    iconRes = R.drawable.profile,
                    iconFillRes = R.drawable.profile_fill,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    }
}

/** Tab SOS khusus: lingkaran biru (rounded penuh) + border 8dp. */
@Composable
private fun SosBottomNavItem(
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .border(6.dp, PrimaryBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.sos_menu_fill),
                    contentDescription = label,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: NavTab,
    label: String,
    @DrawableRes iconRes: Int,
    @DrawableRes iconFillRes: Int,
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    val isSelected = selectedTab == tab
    val tintColor = if (isSelected) PrimaryBlue else BarGrey

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTabSelected(tab) }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(if (isSelected) iconFillRes else iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = tintColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
