package com.dicoding.gunungkerinci.Ticket

import com.dicoding.gunungkerinci.model.MyTiketItem
import com.dicoding.gunungkerinci.model.OverstaySummary

/**
 * Fungsi murni pemilihan state/tab dan label status untuk daftar tiket.
 * Dipisahkan dari adapter agar bisa diuji tanpa view.
 */
object TiketStatus {

    /** Status 0–3 dan riwayat batal 9 di Draft; 4–8 di Tiket Saya. */
    fun isDraft(status: Int?): Boolean = status != null && (status in 0..3 || status == 9)

    fun isTicket(status: Int?): Boolean = status != null && status in 4..8

    fun cardLabel(item: MyTiketItem): String = when {
        item.statusBooking == 9 || (item.statusBooking == 3 && item.canContinue == false) -> "Dibatalkan"
        item.statusBooking in 0..2 -> "Selesaikan Pendaftaran"
        item.statusBooking == 3 -> "Menunggu Pembayaran"
        item.statusBooking in 4..6 -> "Aktif"
        item.statusBooking in 7..8 -> "Selesai"
        else -> "Status belum tersedia"
    }

    /**
     * Label tambahan (overstay) untuk kartu tiket.
     * - masih ada pendaki lewat jadwal turun → "Lewat jadwal turun";
     * - ada tagihan final belum lunas → "Biaya tambahan belum dibayar";
     * - ada biaya menunggu pemeriksaan petugas (needs_review) → label tersendiri.
     * Kedua kondisi dapat muncul bersamaan; tidak saling menyembunyikan.
     */
    fun overstayLabels(os: OverstaySummary?): List<String> {
        if (os == null) return emptyList()
        val team = os.team
        val labels = mutableListOf<String>()
        if ((team?.overdueActiveCount ?: 0) > 0) labels += "Lewat jadwal turun"
        if ((team?.unpaidFinalCount ?: 0) > 0) labels += "Biaya tambahan belum dibayar"
        if (os.pendakis.orEmpty().any { it.chargeStatus == "needs_review" }) {
            labels += "Biaya menunggu pemeriksaan petugas"
        }
        return labels
    }

    /** Nama ketua/pemesan: pendaki pertama, fallback "-". */
    fun ketuaName(item: MyTiketItem): String {
        val bio = item.pendakis?.firstOrNull()?.biodata
        return listOf(bio?.firstName, bio?.lastName)
            .filterNotNull()
            .joinToString(" ")
            .trim()
            .ifBlank { "-" }
    }

    /** "2 WNI + 1 WNA" atau "-". */
    fun pendakiLabel(item: MyTiketItem): String {
        val wni = item.totalPendakiWni ?: 0
        val wna = item.totalPendakiWna ?: 0
        val parts = mutableListOf<String>()
        if (wni > 0) parts += "$wni WNI"
        if (wna > 0) parts += "$wna WNA"
        return if (parts.isEmpty()) "-" else parts.joinToString(" + ")
    }
}
