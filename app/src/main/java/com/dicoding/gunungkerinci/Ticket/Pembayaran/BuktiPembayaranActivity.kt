package com.dicoding.gunungkerinci.Ticket.Pembayaran

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.Barcode.BarcodeTiketActivity
import com.dicoding.gunungkerinci.databinding.ActivityBuktiPembayaranBinding
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.model.BookingDetailData
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

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

        // BUTTON BACK
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // BUTTON BERANDA
        binding.btnBeranda.setOnClickListener {
            //val intent = Intent(this, MainActivity::class.java)
            val intent = Intent(this, BarcodeTiketActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
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
            }

            else -> {
                binding.tvStatus.text =
                    "Menunggu"

                binding.tvStatus.setBackgroundResource(
                    R.drawable.bg_status_menunggu
                )

                binding.layoutApproved.visibility =
                    View.GONE
            }
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
}