package com.dicoding.gunungkerinci.model

import com.google.gson.annotations.SerializedName

/**
 * Respons `GET /api/booking/{id}/tiket` — sumber detail tiket + QR.
 * Mengandung `unique_code` untuk QR serta ringkasan keterlambatan (overstay)
 * per pendaki. Nilai yang belum diketahui bersifat null, bukan nol.
 */
data class TiketResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: TiketData? = null,
    @SerializedName("errors") val errors: Any? = null
)

data class TiketData(
    @SerializedName("id") val id: String,
    @SerializedName("unique_code") val uniqueCode: String? = null,
    @SerializedName("tanggal_masuk") val tanggalMasuk: String? = null,
    @SerializedName("tanggal_keluar") val tanggalKeluar: String? = null,
    @SerializedName("total_pendaki_wni") val totalPendakiWni: Int? = null,
    @SerializedName("total_pendaki_wna") val totalPendakiWna: Int? = null,
    @SerializedName("status_booking") val statusBooking: Int? = null,
    @SerializedName("gate_masuk") val gateMasuk: TiketGate? = null,
    @SerializedName("gate_keluar") val gateKeluar: TiketGate? = null,
    @SerializedName("pendakis") val pendakis: List<TiketAnggota>? = null,
    @SerializedName("overstay") val overstay: OverstaySummary? = null,
    @SerializedName("server_now") val serverNow: String? = null,
    @SerializedName("timezone") val timezone: String? = null
)

data class TiketGate(
    @SerializedName("nama") val nama: String? = null
)

data class TiketAnggota(
    @SerializedName("id") val id: String,
    @SerializedName("biodata") val biodata: TiketBiodata? = null
)

data class TiketBiodata(
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null
)

data class OverstaySummary(
    @SerializedName("booking_id") val bookingId: String? = null,
    @SerializedName("unique_code") val uniqueCode: String? = null,
    @SerializedName("scheduled_exit_date") val scheduledExitDate: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("server_now") val serverNow: String? = null,
    @SerializedName("pendakis") val pendakis: List<OverstayPendaki>? = null,
    @SerializedName("team") val team: OverstayTeam? = null
)

data class OverstayPendaki(
    @SerializedName("id") val id: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("kategori") val kategori: String? = null,
    @SerializedName("hike_state") val hikeState: String? = null,
    @SerializedName("scheduled_exit_date") val scheduledExitDate: String? = null,
    @SerializedName("actual_checkout_at") val actualCheckoutAt: String? = null,
    @SerializedName("overdue_days") val overdueDays: Int? = null,
    @SerializedName("is_estimate") val isEstimate: Boolean? = null,
    @SerializedName("amount") val amount: Int? = null,
    @SerializedName("charge_status") val chargeStatus: String? = null,
    @SerializedName("payment_state") val paymentState: String? = null,
    @SerializedName("paid_at") val paidAt: String? = null,
    @SerializedName("items") val items: List<OverstayItem>? = null
)

data class OverstayItem(
    @SerializedName("tanggal") val tanggal: String? = null,
    @SerializedName("day_type") val dayType: String? = null,
    @SerializedName("kategori") val kategori: String? = null,
    @SerializedName("harga") val harga: Int? = null
)

data class OverstayTeam(
    @SerializedName("overdue_active_count") val overdueActiveCount: Int? = null,
    @SerializedName("unpaid_final_count") val unpaidFinalCount: Int? = null,
    @SerializedName("estimate_total") val estimateTotal: Int? = null,
    @SerializedName("final_total") val finalTotal: Int? = null
)
