package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.MyTiketItem
import com.dicoding.gunungkerinci.network.ApiService

internal data class ActiveHike(
    val bookingId: String,
    val gateId: Int,
    val gateName: String?,
    val statusBooking: Int,
    val pendakiIds: List<String>,
    val tanggalMasuk: String? = null,
    val tanggalKeluar: String? = null
)

internal class ActiveHikeRepository(private val api: ApiService) {

    /** Daftar seluruh booking milik user yang login (GET /mytiket). */
    suspend fun getBookings(token: String): Result<List<MyTiketItem>> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.getMyTiket(bearer)
        val body = response.body()
        if (!response.isSuccessful || body?.success != true) {
            error(body?.message ?: "Gagal mengambil daftar booking (${response.code()})")
        }
        body.data.orEmpty()
    }

    /** Pendakian aktif (booking dengan status_booking = 6). */
    suspend fun getActiveHike(token: String): Result<ActiveHike> =
        getBookings(token).mapCatching { bookings ->
            toActiveHike(bookings)
                ?: error("Belum ada pendakian aktif. Check-in tiket dulu untuk memakai Pelacakan Jejak.")
        }

    companion object {
        private const val STATUS_BOOKING_MENDAKI = 6

        /** Fungsi murni: pilih booking yang sedang mendaki dari daftar (tanpa panggilan API). */
        fun toActiveHike(bookings: List<MyTiketItem>): ActiveHike? {
            val booking = bookings.firstOrNull { it.statusBooking == STATUS_BOOKING_MENDAKI }
                ?: return null
            val gateId = booking.gateMasuk?.id ?: return null
            return ActiveHike(
                bookingId = booking.id,
                gateId = gateId,
                gateName = booking.gateMasuk.nama,
                statusBooking = booking.statusBooking ?: 0,
                pendakiIds = booking.pendakis.orEmpty().map { it.id },
                tanggalMasuk = booking.tanggalMasuk,
                tanggalKeluar = booking.tanggalKeluar
            )
        }
    }
}
