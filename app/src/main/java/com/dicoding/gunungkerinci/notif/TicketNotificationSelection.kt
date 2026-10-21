package com.dicoding.gunungkerinci.notif

import com.dicoding.gunungkerinci.model.MyTiketItem
import com.dicoding.gunungkerinci.model.OverstaySummary
import com.dicoding.gunungkerinci.util.DateTimeFormat

enum class TicketEventType { REMINDER, OVERDUE }

data class TicketEvent(
    val type: TicketEventType,
    val bookingId: String,
    /** Kunci stabil per akun/booking/jenis kejadian untuk dedup dan id notifikasi. */
    val eventKey: String,
    val title: String,
    val body: String
)

/**
 * Fungsi murni seleksi notifikasi lokal tiket. Dipakai bersama oleh worker
 * background dan (bila perlu) pemuatan foreground agar aturan tidak diduplikasi.
 *
 * Waktu hanya diambil dari `server_now` backend (Asia/Jakarta), bukan jam HP.
 * `server_now` berformat ISO 8601 dengan offset +07:00, sehingga 10 karakter
 * pertama adalah tanggal WIB dan karakter 11..15 adalah jam WIB.
 */
object TicketNotificationSelection {

    const val REMINDER_TITLE = "Jadwal turun hari ini"
    const val REMINDER_BODY =
        "Jadwal turun Anda hari ini. Konfirmasikan kepulangan kepada petugas saat tiba di pos."
    const val OVERDUE_TITLE = "Durasi tiket telah berakhir"
    const val OVERDUE_BODY =
        "Durasi tiket telah berakhir. Buka tiket untuk melihat status kepulangan dan estimasi biaya tambahan."

    fun serverDate(serverNowIso: String?): String? = DateTimeFormat.apiWibDate(serverNowIso)

    fun serverTime(serverNowIso: String?): String? = DateTimeFormat.apiWibTimestamp(serverNowIso)?.substring(11, 16)

    /** Masih ada pendaki yang status terakhirnya mendaki (belum checkout). */
    fun hasActivePendaki(os: OverstaySummary?): Boolean =
        os?.pendakis.orEmpty().any { it.hikeState == "mendaki" }

    /**
     * Pilih kejadian notifikasi untuk akun pemilik [myUserId].
     * `alreadyShown` berisi eventKey yang sudah pernah ditampilkan (dedup).
     */
    fun selectEvents(
        bookings: List<MyTiketItem>,
        myUserId: Int?,
        serverNowIso: String?,
        reminderTime: String,
        accountKey: String,
        alreadyShown: Set<String>
    ): List<TicketEvent> {
        val today = serverDate(serverNowIso) ?: return emptyList()
        val time = serverTime(serverNowIso) ?: return emptyList()
        val afterReminder = time >= reminderTime

        return bookings
            .filter { it.idUser != null && it.idUser == myUserId }
            .filter { (it.statusBooking ?: -1) in 4..8 }
            .mapNotNull { booking ->
                val exit = booking.tanggalKeluar?.take(10) ?: return@mapNotNull null
                if (!hasActivePendaki(booking.overstay)) return@mapNotNull null

                when {
                    exit == today && afterReminder -> {
                        val key = "$accountKey:reminder:${booking.id}:$exit"
                        if (key in alreadyShown) null
                        else TicketEvent(TicketEventType.REMINDER, booking.id, key, REMINDER_TITLE, REMINDER_BODY)
                    }

                    exit < today -> {
                        val key = "$accountKey:overdue:${booking.id}"
                        if (key in alreadyShown) null
                        else TicketEvent(TicketEventType.OVERDUE, booking.id, key, OVERDUE_TITLE, OVERDUE_BODY)
                    }

                    else -> null
                }
            }
    }
}
