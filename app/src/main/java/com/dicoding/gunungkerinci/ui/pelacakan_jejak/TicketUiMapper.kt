package com.dicoding.gunungkerinci.ui.pelacakan_jejak

import com.dicoding.gunungkerinci.model.MyTiketItem
import java.util.Calendar
import java.util.Locale

/**
 * Memetakan booking dari `GET /mytiket` menjadi kartu tiket di layar Jejak.
 *
 * Arti `status_booking` (backend `gk_bookings`):
 * 1 SNK · 2 Formulir · 3 Menunggu Pembayaran · 4 Sudah Bayar · 5 Konfirmasi ·
 * 6 Check-in (sedang mendaki) · 7 Check-out · 8 Selesai
 */
internal object TicketUiMapper {

    private const val STATUS_MENDAKI = 6
    private val STATUS_SELESAI = listOf(7, 8)

    private val NAMA_BULAN = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    /** Booking yang sedang mendaki (status 6) → kartu "Tiket Aktif". */
    fun toActiveTicket(bookings: List<MyTiketItem>): JejakTicketUi? =
        bookings.firstOrNull { it.statusBooking == STATUS_MENDAKI }?.let { booking ->
            JejakTicketUi(
                title = "Tiket Aktif",
                purchaseLabel = "Pembelian",
                purchaseDate = formatTanggal(booking.createdAt),
                status = "Sedang Mendaki",
                statusType = TicketStatusType.Ongoing,
                masukDate = formatTanggal(booking.tanggalMasuk),
                keluarDate = formatTanggal(booking.tanggalKeluar),
                pendakiCount = formatPendaki(booking),
                totalPayment = formatRupiah(booking.totalPembayaran),
                bookingId = "ID Pemesanan: ${shortId(booking.id)}",
                primaryAction = "Check Point"
            )
        }

    /** Booking yang sudah check-out / selesai (status 7–8) → kartu "Tiket Selesai". */
    fun toFinishedTickets(bookings: List<MyTiketItem>): List<JejakTicketUi> =
        bookings.filter { it.statusBooking in STATUS_SELESAI }.map { booking ->
            JejakTicketUi(
                title = "Tiket Selesai",
                purchaseLabel = "Tiket",
                purchaseDate = formatTanggal(booking.createdAt),
                status = if (booking.statusBooking == 8) "Selesai" else "Check Out",
                statusType = TicketStatusType.Done,
                masukDate = formatTanggal(booking.tanggalMasuk),
                keluarDate = formatTanggal(booking.tanggalKeluar),
                pendakiCount = formatPendaki(booking),
                totalPayment = formatRupiah(booking.totalPembayaran),
                bookingId = "ID Pemesanan: ${shortId(booking.id)}",
                primaryAction = "Detail"
            )
        }

    private fun shortId(id: String): String = id.take(8).uppercase(Locale.ROOT)

    private val NAMA_BULAN_SINGKAT = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    /**
     * Daftar hari pendakian dari tanggal masuk s.d. keluar → chip "Hari 1 / Hari 2 / …".
     * Hari pertama ditandai terpilih. Memakai [java.util.Calendar] agar tetap jalan di minSdk 24
     * (java.time baru tersedia API 26 tanpa desugaring).
     */
    fun toDays(tanggalMasuk: String?, tanggalKeluar: String?): List<JejakDayUi> {
        val mulai = toCalendar(tanggalMasuk) ?: return emptyList()
        val selesai = toCalendar(tanggalKeluar) ?: mulai
        val hari = mutableListOf<JejakDayUi>()
        var kursor = mulai
        var nomor = 1
        while (!kursor.after(selesai) && nomor <= 30) {
            hari += JejakDayUi(
                title = "Hari $nomor",
                date = "${kursor.get(Calendar.DAY_OF_MONTH)} ${NAMA_BULAN_SINGKAT[kursor.get(Calendar.MONTH)]}",
                selected = nomor == 1
            )
            kursor = (kursor.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            nomor++
        }
        return hari
    }

    /** "2026-09-13" → "September 2026" (label periode di atas timeline). */
    fun toPeriodeLabel(tanggalMasuk: String?): String? {
        val kalender = toCalendar(tanggalMasuk) ?: return null
        return "${NAMA_BULAN[kalender.get(Calendar.MONTH)]} ${kalender.get(Calendar.YEAR)}"
    }

    private fun toCalendar(iso: String?): Calendar? {
        val bagian = iso?.take(10)?.split("-") ?: return null
        if (bagian.size != 3) return null
        val tahun = bagian[0].toIntOrNull() ?: return null
        val bulan = bagian[1].toIntOrNull() ?: return null
        val tanggal = bagian[2].toIntOrNull() ?: return null
        return Calendar.getInstance().apply { set(tahun, bulan - 1, tanggal) }
    }

    /** "2026-09-13" atau "2026-09-12T04:31:25.000000Z" → "13 September 2026". */
    fun formatTanggal(iso: String?): String {
        val tanggal = iso?.take(10) ?: return "-"
        val bagian = tanggal.split("-")
        if (bagian.size != 3) return tanggal
        val bulan = bagian[1].toIntOrNull()?.minus(1)?.let { NAMA_BULAN.getOrNull(it) } ?: return tanggal
        val hari = bagian[2].toIntOrNull() ?: return tanggal
        return "$hari $bulan ${bagian[0]}"
    }

    /** 2 pendaki (WNI + WNA) → "2 Orang". */
    private fun formatPendaki(booking: MyTiketItem): String {
        val jumlah = (booking.totalPendakiWni ?: 0) + (booking.totalPendakiWna ?: 0)
        return if (jumlah > 0) "$jumlah Orang" else "-"
    }

    /** 105000 → "Rp 105.000". */
    fun formatRupiah(nilai: Int?): String {
        val angka = nilai ?: 0
        val ribuan = angka.toString().reversed().chunked(3).joinToString(".").reversed()
        return "Rp $ribuan"
    }
}
