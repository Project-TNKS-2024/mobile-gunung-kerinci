package com.dicoding.gunungkerinci.data.repository

import com.dicoding.gunungkerinci.model.MyTiketItem
import com.dicoding.gunungkerinci.network.ApiService

internal data class ActiveHike(
    val bookingId: String,
    val gateId: Int,
    val gateName: String?,
    val statusBooking: Int,
    val pendakiIds: List<String>,
    /** Id user pemilik booking — menurut server, pemilik booking inilah ketua tim. */
    val ketuaUserId: Int? = null,
    val tanggalMasuk: String? = null,
    val tanggalKeluar: String? = null
)

/** Identitas user yang login: id user dan daftar pendaki miliknya. */
internal data class IdentitasSaya(
    val userId: Int?,
    val pendakiIds: Set<String>
)

internal class ActiveHikeRepository(
    private val api: ApiService,
    private val sessionStore: CheckpointSessionStore? = null
) {

    /** Daftar seluruh booking milik user yang login (GET /mytiket). */
    suspend fun getBookings(token: String): Result<List<MyTiketItem>> = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.getMyTiket(bearer)
        val body = response.body()
        if (!response.isSuccessful || body?.success != true) {
            error(body?.message ?: "Gagal mengambil daftar booking (${response.code()})")
        }
        body.data.orEmpty().also { sessionStore?.saveBookings(token, it) }
    }.recoverCatching { error ->
        if (error is java.io.IOException) sessionStore?.readBookings(token) ?: throw error else throw error
    }

    /** Pendakian aktif (booking dengan status_booking = 6). */
    suspend fun getActiveHike(token: String): Result<ActiveHike> =
        getBookings(token).mapCatching { bookings ->
            toActiveHike(bookings)
                ?: error("Belum ada pendakian aktif. Check-in tiket dulu untuk memakai Pelacakan Jejak.")
        }

    /**
     * Identitas user yang login (GET /profile/pendaki-identity): id user + id pendaki
     * miliknya. Id user dipakai untuk tahu apakah dia ketua tim pada booking aktif.
     */
    suspend fun getMyIdentity(token: String): IdentitasSaya = runCatching {
        val bearer = if (token.startsWith("Bearer ")) token else "Bearer $token"
        val response = api.getPendakiIdentity(bearer)
        val body = response.body()
        if (!response.isSuccessful || body?.success != true) error("Gagal memuat identitas pendaki")
        IdentitasSaya(
            userId = body.data?.id_user,
            pendakiIds = body.data?.pendaki_ids.orEmpty().toSet()
        ).also { sessionStore?.saveIdentity(token, it) }
    }.getOrElse { error ->
        if (error is java.io.IOException) sessionStore?.readIdentity(token) ?: IdentitasSaya(null, emptySet())
        else IdentitasSaya(null, emptySet())
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
                ketuaUserId = booking.idUser,
                tanggalMasuk = booking.tanggalMasuk,
                tanggalKeluar = booking.tanggalKeluar
            )
        }

        /**
         * Fungsi murni: pilih anggota rombongan yang cocok dengan user yang login.
         * Kalau tidak ada yang cocok, pakai yang pertama (jaga-jaga) — lihat A16.
         */
        fun pilihPendakiSaya(pendakiIds: List<String>, myPendakiIds: Set<String>): String =
            pendakiIds.firstOrNull { it in myPendakiIds } ?: pendakiIds.firstOrNull().orEmpty()
    }
}
