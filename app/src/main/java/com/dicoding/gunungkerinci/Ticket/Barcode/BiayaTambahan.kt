package com.dicoding.gunungkerinci.Ticket.Barcode

import com.dicoding.gunungkerinci.model.OverstayPendaki
import com.dicoding.gunungkerinci.model.OverstaySummary

/**
 * Fungsi murni untuk merangkum rincian biaya tambahan. Dipisahkan dari sheet
 * agar perhitungan "final vs unpaid" dapat diuji tanpa view.
 */
object BiayaTambahan {

    fun dayTypeLabel(dayType: String?): String = when (dayType) {
        "weekday" -> "Hari biasa"
        "weekend" -> "Akhir pekan"
        "holiday" -> "Hari libur"
        else -> "-"
    }

    fun kategoriLabel(kategori: String?): String = when (kategori) {
        "wni" -> "WNI"
        "wna" -> "WNA"
        else -> "-"
    }

    fun hikeStateLabel(state: String?): String = when (state) {
        "mendaki" -> "Sedang mendaki"
        "turun" -> "Sudah turun"
        "batal" -> "Batal mendaki"
        else -> "Belum masuk"
    }

    /**
     * Apakah ada rincian biaya yang perlu ditampilkan: estimasi, tagihan final,
     * atau menunggu pemeriksaan petugas.
     */
    fun hasInfo(os: OverstaySummary?): Boolean =
        os?.pendakis.orEmpty().any { it.chargeStatus in setOf("estimate", "final", "needs_review") }

    fun hasEstimate(os: OverstaySummary?): Boolean =
        os?.pendakis.orEmpty().any { it.chargeStatus == "estimate" }

    fun hasFinal(os: OverstaySummary?): Boolean =
        os?.pendakis.orEmpty().any { it.chargeStatus == "final" }

    fun hasNeedsReview(os: OverstaySummary?): Boolean =
        os?.pendakis.orEmpty().any { it.chargeStatus == "needs_review" }

    /**
     * Total final yang BELUM dibayar: hanya item `final` dengan
     * `payment_state=unpaid` dan nominal diketahui. Kembali null bila ada
     * `needs_review` sehingga total final belum lengkap (bukan nol pasti).
     */
    fun unpaidFinalTotal(pendakis: List<OverstayPendaki>?): Int? {
        val list = pendakis.orEmpty()
        val incomplete = list.any { it.chargeStatus == "needs_review" }
        val sum = list
            .filter { it.chargeStatus == "final" && it.paymentState == "unpaid" }
            .sumOf { it.amount ?: 0 }
        return if (incomplete) null else sum
    }
}
