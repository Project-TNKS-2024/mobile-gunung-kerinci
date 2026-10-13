package com.dicoding.gunungkerinci.Ticket.Pembayaran

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityBuktiPembayaranBinding
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.model.BookingDetailData
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import com.dicoding.gunungkerinci.Ticket.Barcode.BarcodeTiketActivity

class BuktiPembayaranActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuktiPembayaranBinding

    private var bookingId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuktiPembayaranBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingId =
            intent.getStringExtra("booking_id").orEmpty()

        if (bookingId.isBlank()) {

            Toast.makeText(
                this,
                "Booking ID tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        ambilStatusPembayaran()

        setupButtonLihatTiket()
        ambilStatusPembayaran()

        // BUTTON BACK
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // BUTTON BERANDA
        binding.btnBeranda.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        binding.textUnduh.setOnClickListener {
            val url = ApiConfig.getInvoiceUrl(bookingId)

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

    }

    private fun setupButtonLihatTiket() {
        binding.btnLihatTiket.setOnClickListener {
            if (bookingId.isBlank()) {
                Toast.makeText(
                    this,
                    "Booking ID tidak ditemukan",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val intent = Intent(
                this,
                BarcodeTiketActivity::class.java
            )

            intent.putExtra("booking_id", bookingId)
            startActivity(intent)
        }
    }

    private fun ambilStatusPembayaran() {

        lifecycleScope.launch {

            try {

                Log.d(
                    "STATUS_PEMBAYARAN",
                    "Booking ID: $bookingId"
                )

                val response =
                    ApiConfig
                        .getApiService(
                            this@BuktiPembayaranActivity
                        )
                        .getPayment(bookingId)

                Log.d(
                    "STATUS_PEMBAYARAN",
                    "HTTP Code: ${response.code()}"
                )

                if (!response.isSuccessful) {

                    Log.e(
                        "STATUS_PEMBAYARAN",
                        "Error: ${response.errorBody()?.string()}"
                    )

                    Toast.makeText(
                        this@BuktiPembayaranActivity,
                        "Gagal mengambil status pembayaran",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val body = response.body()

                Log.d(
                    "STATUS_PEMBAYARAN",
                    "Response: $body"
                )

                if (body?.success != true) {

                    Toast.makeText(
                        this@BuktiPembayaranActivity,
                        body?.message
                            ?: "Status pembayaran tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val data = body.data

                if (data == null) {

                    Toast.makeText(
                        this@BuktiPembayaranActivity,
                        "Data pembayaran kosong",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                tampilkanRincianPemesanan(data.booking)

                val pembayaran =
                    data.booking.pembayaran
                        ?.lastOrNull()

                if (pembayaran == null) {

                    Log.d(
                        "STATUS_PEMBAYARAN",
                        "Belum ada data pembayaran"
                    )

                    return@launch
                }

                Log.d(
                    "STATUS_PEMBAYARAN",
                    "Payment ID: ${pembayaran.id}"
                )

                Log.d(
                    "STATUS_PEMBAYARAN",
                    "Status: ${pembayaran.status}"
                )

                Log.d(
                    "STATUS_PEMBAYARAN",
                    "Metode: ${pembayaran.payment_method}"
                )

                Log.d(
                    "STATUS_PEMBAYARAN",
                    "Deadline: ${pembayaran.deadline}"
                )

                tampilkanStatusPembayaran(
                    pembayaran.status
                )

                tampilkanRiwayatPembayaran(
                    data.booking,
                    pembayaran.status
                )

                binding.btnLihatBukti.setOnClickListener {

                    if (pembayaran.status?.lowercase() == "success") {

                        val buktiPembayaran =
                            pembayaran.bukti_pembayaran

                        if (buktiPembayaran.isNullOrBlank()) {

                            Toast.makeText(
                                this@BuktiPembayaranActivity,
                                "Bukti pembayaran tidak ditemukan",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@setOnClickListener
                        }

                        bukaBuktiPembayaran(
                            buktiPembayaran
                        )

                    } else {

                        Toast.makeText(
                            this@BuktiPembayaranActivity,
                            "Bukti pembayaran belum disetujui",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "STATUS_PEMBAYARAN",
                    "Exception: ${e.message}",
                    e
                )

                Toast.makeText(
                    this@BuktiPembayaranActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun tampilkanStatusPembayaran(status: String?) {

        when (status?.lowercase()) {
            "pending" -> {
                binding.tvStatus.text =
                    "Menunggu"

                binding.tvStatus.setBackgroundResource(
                    R.drawable.bg_status_menunggu
                )

                binding.layoutApproved.visibility =
                    View.GONE

                binding.btnLihatTiket.visibility =
                    View.GONE
            }

            "success" -> {
                binding.tvStatus.text =
                    "Disetujui"

                binding.tvStatus.setBackgroundColor(
                    Color.parseColor("#4CB050")
                )

                binding.tvStatus.setTextColor(
                    Color.WHITE
                )

                binding.layoutApproved.visibility =
                    View.VISIBLE

                binding.btnLihatTiket.visibility =
                    View.VISIBLE

                Toast.makeText(
                    this,
                    "Pembayaran berhasil diverifikasi",
                    Toast.LENGTH_SHORT
                ).show()
            }

            "failed" -> {
                binding.tvStatus.text =
                    "Dibatalkan"

                binding.tvStatus.setBackgroundColor(
                    Color.parseColor("#E53935")
                )

                binding.tvStatus.setTextColor(
                    Color.WHITE
                )

                binding.layoutApproved.visibility =
                    View.GONE

                binding.btnLihatTiket.visibility =
                    View.GONE
            }

            else -> {
                binding.tvStatus.text =
                    "Menunggu"

                binding.tvStatus.setBackgroundResource(
                    R.drawable.bg_status_menunggu
                )

                binding.layoutApproved.visibility =
                    View.GONE

                binding.btnLihatTiket.visibility =
                    View.GONE
            }
        }
    }

    private fun tampilkanRiwayatPembayaran(
        booking: BookingDetailData,
        status: String?
    ) {

        try {

            val dataStrukJson = booking.dataStruk

            if (dataStrukJson.isNullOrBlank()) {
                Log.e(
                    "RIWAYAT_PEMBAYARAN",
                    "dataStruk kosong"
                )

                binding.tvTanggal.text = "-"
                binding.tvKet.text = "-"

                return
            }

            val jsonObject =
                org.json.JSONObject(dataStrukJson)

            val pembayaranArray =
                jsonObject.optJSONArray("pembayaran")

            if (pembayaranArray == null ||
                pembayaranArray.length() == 0
            ) {

                Log.e(
                    "RIWAYAT_PEMBAYARAN",
                    "Array pembayaran kosong"
                )

                binding.tvTanggal.text = "-"
                binding.tvKet.text = "-"

                return
            }

            // Ambil pembayaran terakhir
            val pembayaranTerakhir =
                pembayaranArray.getJSONObject(
                    pembayaranArray.length() - 1
                )

            val createdAt =
                pembayaranTerakhir.optString(
                    "created_at",
                    ""
                )

            val updatedAt =
                pembayaranTerakhir.optString(
                    "updated_at",
                    ""
                )

            val keterangan =
                pembayaranTerakhir.optString(
                    "keterangan",
                    ""
                )

            val statusPembayaran =
                status
                    ?.lowercase()
                    .orEmpty()

            /*
             * Pending:
             * gunakan waktu created_at
             *
             * Success / Failed:
             * gunakan waktu updated_at
             */
            val tanggalYangDipakai =
                when (statusPembayaran) {

                    "pending" -> createdAt

                    "success",
                    "failed" -> updatedAt

                    else -> createdAt
                }

            binding.tvTanggal.text =
                formatTanggalPembayaran(
                    tanggalYangDipakai
                )

            binding.tvKet.text =
                if (keterangan.isNotBlank()) {
                    keterangan
                } else {
                    "-"
                }

            Log.d(
                "RIWAYAT_PEMBAYARAN",
                "Status: $statusPembayaran"
            )

            Log.d(
                "RIWAYAT_PEMBAYARAN",
                "Created At: $createdAt"
            )

            Log.d(
                "RIWAYAT_PEMBAYARAN",
                "Updated At: $updatedAt"
            )

            Log.d(
                "RIWAYAT_PEMBAYARAN",
                "Tanggal ditampilkan: $tanggalYangDipakai"
            )

            Log.d(
                "RIWAYAT_PEMBAYARAN",
                "Keterangan: $keterangan"
            )

        } catch (e: Exception) {

            Log.e(
                "RIWAYAT_PEMBAYARAN",
                "Gagal menampilkan riwayat pembayaran: ${e.message}",
                e
            )

            binding.tvTanggal.text = "-"
            binding.tvKet.text = "-"
        }
    }

    private fun formatTanggalPembayaran(
        tanggal: String?
    ): String {

        if (tanggal.isNullOrBlank()) {
            return "-"
        }

        return try {

            val inputFormat =
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                    Locale.US
                )

            val outputFormat =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale("id", "ID")
                )

            val date =
                inputFormat.parse(tanggal)

            if (date != null) {
                outputFormat.format(date)
            } else {
                tanggal
            }

        } catch (e: Exception) {

            Log.e(
                "FORMAT_TANGGAL",
                "Gagal format tanggal: $tanggal",
                e
            )

            tanggal
        }
    }

    private fun tampilkanRincianPemesanan(
        booking: BookingDetailData
    ) {

        // ==============================
        // NAMA PEMESAN
        // ==============================

        try {

            val dataStrukJson =
                booking.dataStruk

            if (!dataStrukJson.isNullOrBlank()) {

                val jsonObject =
                    org.json.JSONObject(dataStrukJson)

                val user =
                    jsonObject.optJSONObject("user")

                val biodata =
                    user?.optJSONObject("biodata")

                val namaPemesan =
                    biodata?.optString(
                        "fullName",
                        ""
                    ).orEmpty()

                binding.tvNamaPemesan.text =
                    if (namaPemesan.isNotBlank()) {
                        namaPemesan
                    } else {
                        "-"
                    }
            } else {

                binding.tvNamaPemesan.text =
                    "-"
            }

        } catch (e: Exception) {

            Log.e(
                "RINCIAN_PEMESANAN",
                "Gagal mengambil nama pemesan: ${e.message}",
                e
            )

            binding.tvNamaPemesan.text =
                "-"
        }


        // ==============================
        // GERBANG MASUK
        // ==============================

        binding.tvGerbangMasuk.text =
            booking.gate_masuk.nama


        // ==============================
        // GERBANG KELUAR
        // ==============================

        binding.tvGerbangKeluar.text =
            booking.gate_keluar.nama


        // ==============================
        // TANGGAL MASUK
        // ==============================

        binding.tvTanggalMasuk.text =
            formatTanggal(
                booking.tanggal_masuk
            )


        // ==============================
        // TANGGAL KELUAR
        // ==============================

        binding.tvTanggalKeluar.text =
            formatTanggal(
                booking.tanggal_keluar
            )


        Log.d(
            "RINCIAN_PEMESANAN",
            "Nama Pemesan: ${binding.tvNamaPemesan.text}"
        )

        Log.d(
            "RINCIAN_PEMESANAN",
            "Gerbang Masuk: ${binding.tvGerbangMasuk.text}"
        )

        Log.d(
            "RINCIAN_PEMESANAN",
            "Gerbang Keluar: ${binding.tvGerbangKeluar.text}"
        )

        Log.d(
            "RINCIAN_PEMESANAN",
            "Tanggal Masuk: ${binding.tvTanggalMasuk.text}"
        )

        Log.d(
            "RINCIAN_PEMESANAN",
            "Tanggal Keluar: ${binding.tvTanggalKeluar.text}"
        )
    }

    private fun formatTanggal(
        tanggal: String?
    ): String {

        if (tanggal.isNullOrBlank()) {
            return "-"
        }

        return try {

            val input =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val output =
                SimpleDateFormat(
                    "dd MMMM yyyy",
                    Locale("id", "ID")
                )

            output.format(
                input.parse(tanggal)!!
            )

        } catch (e: Exception) {

            tanggal
        }
    }

    private fun bukaBuktiPembayaran(
        pathBukti: String
    ) {

        try {

            val baseUrl =
                "http://192.168.137.51:8000/"

            val url =
                baseUrl + pathBukti

            Log.d(
                "BUKTI_PEMBAYARAN",
                "URL bukti: $url"
            )

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )

            startActivity(intent)

        } catch (e: Exception) {

            Log.e(
                "BUKTI_PEMBAYARAN",
                "Gagal membuka bukti pembayaran: ${e.message}",
                e
            )

            Toast.makeText(
                this,
                "Tidak dapat membuka bukti pembayaran",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}