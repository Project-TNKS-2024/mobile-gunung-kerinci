package com.dicoding.gunungkerinci.Ticket.Barcode

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.TranslateAnimation
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.databinding.ActivityBarcodeTiketBinding
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import com.dicoding.gunungkerinci.model.BookingDetailData
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class BarcodeTiketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarcodeTiketBinding
    private var indicatorWidth = 0
    private var startPositionTim = 0
    private var startPositionIndividu = 0

    private var bookingId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeTiketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingId = intent.getStringExtra("booking_id").orEmpty()

        if (bookingId.isNotBlank()) {
            ambilDetailBooking(bookingId)
        } else {
            Toast.makeText(
                this,
                "ID booking tidak ditemukan",
                Toast.LENGTH_LONG
            ).show()
        }

        setupInitialIndicator()
        setupClickMenu()
        setupNavigationButtons()

        tesApiMyTiket()
    }

    private fun buatQrCode(teks: String): Bitmap {
        val ukuran = 900

        val bitMatrix = QRCodeWriter().encode(
            teks,
            BarcodeFormat.QR_CODE,
            ukuran,
            ukuran
        )

        val bitmap = Bitmap.createBitmap(
            ukuran,
            ukuran,
            Bitmap.Config.RGB_565
        )

        for (x in 0 until ukuran) {
            for (y in 0 until ukuran) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                )
            }
        }

        return bitmap
    }

    private fun tampilkanTiketTim(booking: BookingDetailData) {
        val layoutTim = binding.layoutTim

        val imgQr = layoutTim.findViewById<ImageView>(
            R.id.rvQRCodeTim
        )

        val tvNamaPemesan = layoutTim.findViewById<TextView>(
            R.id.tvNamaPemesan
        )

        val tvNoSeri = layoutTim.findViewById<TextView>(
            R.id.textNoSeri
        )

        val tvGerbangMasuk = layoutTim.findViewById<TextView>(
            R.id.tvGerbangMasuk
        )

        val tvGerbangKeluar = layoutTim.findViewById<TextView>(
            R.id.tvGerbangKeluar
        )

        val tvTanggalMasuk = layoutTim.findViewById<TextView>(
            R.id.tvTanggalMasuk
        )

        val tvTanggalKeluar = layoutTim.findViewById<TextView>(
            R.id.tvTanggalKeluar
        )

        val tvJumlahAnggota = layoutTim.findViewById<TextView>(
            R.id.tvJumlahAnggota
        )

        val tvKodePemesanan = layoutTim.findViewById<TextView>(
            R.id.tvKodePemesananTim
        )

        tvKodePemesanan.text =
            booking.unique_code.orEmpty().ifBlank { "-" }

        // Ambil nama pemesan dari dataStruk
        val namaPemesan = try {
            val dataStruk = booking.dataStruk

            if (!dataStruk.isNullOrBlank()) {
                val json = JSONObject(dataStruk)
                json.optJSONObject("user")
                    ?.optJSONObject("biodata")
                    ?.optString("fullName")
                    .orEmpty()
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }

        tvNamaPemesan.text =
            namaPemesan.ifBlank { "-" }

        // Nomor seri sementara berdasarkan ID booking
        tvNoSeri.text =
            booking.unique_code.orEmpty().ifBlank { "-" }

        // Gerbang
        tvGerbangMasuk.text = booking.gate_masuk.nama
        tvGerbangKeluar.text = booking.gate_keluar.nama

        // Tanggal
        tvTanggalMasuk.text = formatTanggalTiket(booking.tanggal_masuk)
        tvTanggalKeluar.text = formatTanggalTiket(booking.tanggal_keluar)

        // Jumlah anggota
        tvJumlahAnggota.text =
            "${booking.pendakis.size} orang"

        // QR Code untuk tiket TIM
        val uniqueCode = booking.unique_code.orEmpty()

        if (uniqueCode.isNotBlank()) {
            imgQr.setImageBitmap(buatQrCode(uniqueCode))
        } else {
            imgQr.setImageResource(R.drawable.qrcode)

            Log.e(
                "QR_TIM",
                "unique_code tidak tersedia untuk booking ini"
            )
        }
    }

    private fun formatTanggalTiket(tanggal: String): String {
        return try {
            val input = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            )

            val output = SimpleDateFormat(
                "dd MMMM yyyy",
                Locale("id", "ID")
            )

            output.format(input.parse(tanggal)!!)
        } catch (e: Exception) {
            tanggal
        }
    }


    private fun tampilkanTiketIndividu(
        booking: BookingDetailData
    ) {
        val uniqueCode = booking.unique_code.orEmpty()

        val daftarTiket = booking.pendakis.mapIndexed { index, pendaki ->
            val biodata = pendaki.biodata

            TiketModel(
                nama = listOfNotNull(
                    biodata.first_name,
                    biodata.last_name
                ).joinToString(" ").ifBlank {
                    "Pendaki ${index + 1}"
                },

                // Nomor seri memakai unique_code booking
                noSeri = uniqueCode.ifBlank { "-" },

                gerbangMasuk = booking.gate_masuk.nama,
                gerbangKeluar = booking.gate_keluar.nama,
                tanggalMasuk = booking.tanggal_masuk,
                tanggalKeluar = booking.tanggal_keluar,

                // Kode pemesanan memakai unique_code booking
                kodePemesanan = uniqueCode.ifBlank { "-" },

                // QR Code memakai unique_code booking
                qrContent = uniqueCode
            )
        }

        binding.rvIndividu.layoutManager =
            LinearLayoutManager(this)

        binding.rvIndividu.adapter =
            TiketIndividuAdapter(daftarTiket)
    }

    private fun ambilDetailBooking(bookingId: String) {
        lifecycleScope.launch {
            try {
                val apiService =
                    ApiConfig.getApiService(this@BarcodeTiketActivity)

                // Gunakan method detail tiket yang sudah ada di ApiService.
                val response = apiService.getBookingDetail(bookingId)

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body?.success == true) {
                        val booking = body.data

                        tampilkanTiketTim(booking)
                        tampilkanTiketIndividu(booking)

                        Log.d("DETAIL_TIKET", "Booking ID: ${booking.id}")
                        Log.d(
                            "DETAIL_TIKET",
                            "Gerbang masuk: ${booking.gate_masuk.nama}"
                        )
                        Log.d(
                            "DETAIL_TIKET",
                            "Gerbang keluar: ${booking.gate_keluar.nama}"
                        )
                        Log.d(
                            "DETAIL_TIKET",
                            "Jumlah pendaki: ${booking.pendakis.size}"
                        )

                        // Data berhasil diambil.
                        // Tahap berikutnya: tampilkan ke layout TIM dan Individu.
                    } else {
                        Toast.makeText(
                            this@BarcodeTiketActivity,
                            body?.message ?: "Gagal mengambil detail tiket",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Log.e(
                        "DETAIL_TIKET",
                        "HTTP ${response.code()}: ${response.errorBody()?.string()}"
                    )
                    Toast.makeText(
                        this@BarcodeTiketActivity,
                        "Gagal mengambil detail tiket",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("DETAIL_TIKET", "Error mengambil detail", e)
                Toast.makeText(
                    this@BarcodeTiketActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun tesApiMyTiket() {
        lifecycleScope.launch {
            try {
                val apiService = ApiConfig.getApiService(this@BarcodeTiketActivity)
                val response = apiService.getMyTiket()

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body?.success == true) {
                        val daftarBooking = body.data.orEmpty()

                        Log.d(
                            "API_MY_TIKET",
                            "Berhasil mengambil data tiket"
                        )

                        Log.d(
                            "API_MY_TIKET",
                            "Jumlah booking: ${daftarBooking.size}"
                        )

                        Toast.makeText(
                            this@BarcodeTiketActivity,
                            "API berhasil: ${daftarBooking.size} booking",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {
                        Log.e(
                            "API_MY_TIKET",
                            "Response gagal: ${body?.message}"
                        )

                        Toast.makeText(
                            this@BarcodeTiketActivity,
                            body?.message ?: "Gagal mengambil data tiket",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {
                    Log.e(
                        "API_MY_TIKET",
                        "HTTP ${response.code()}: ${response.errorBody()?.string()}"
                    )

                    Toast.makeText(
                        this@BarcodeTiketActivity,
                        "Gagal mengambil tiket: HTTP ${response.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(
                    "API_MY_TIKET",
                    "Error: ${e.message}",
                    e
                )

                Toast.makeText(
                    this@BarcodeTiketActivity,
                    "Koneksi API gagal: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ---------------------------------------------
    // SETUP INDIKATOR AWAL (sesuaikan ukuran menu)
    // ---------------------------------------------
    private fun setupInitialIndicator() {
        binding.menuContainer.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {

                    // Hitung ukuran indicator = lebar menu TIM
                    indicatorWidth = binding.tvMenuTim.width
                    binding.viewIndicator.layoutParams.width = indicatorWidth

                    startPositionTim = binding.tvMenuTim.left
                    startPositionIndividu = binding.tvMenuIndividu.left

                    moveIndicator(startPositionTim)

                    binding.menuContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        )
    }

    // ---------------------------------------------
    // ANIMASI PINDAH INDIKATOR
    // ---------------------------------------------
    private fun moveIndicator(targetX: Int) {
        val animation = TranslateAnimation(
            binding.viewIndicator.translationX,
            targetX.toFloat(),
            0f,
            0f
        )
        animation.duration = 250
        animation.fillAfter = true
        binding.viewIndicator.startAnimation(animation)
    }

    // ---------------------------------------------
    // MENU TIM & INDIVIDU
    // ---------------------------------------------
    private fun setupClickMenu() {

        val primary = getColor(R.color.primary)
        val black2 = getColor(R.color.black2)

        binding.tvMenuTim.setOnClickListener {
            // ubah warna
            binding.tvMenuTim.setTextColor(primary)
            binding.tvMenuIndividu.setTextColor(black2)

            // pindahkan indikator
            moveIndicator(startPositionTim)

            // tampilkan layout TIM
            binding.layoutTim.visibility = View.VISIBLE
            binding.rvIndividu.visibility = View.GONE
        }

        binding.tvMenuIndividu.setOnClickListener {
            // ubah warna
            binding.tvMenuIndividu.setTextColor(primary)
            binding.tvMenuTim.setTextColor(black2)

            // pindahkan indikator
            moveIndicator(startPositionIndividu)

            // tampilkan layout INDIVIDU
            binding.layoutTim.visibility = View.GONE
            binding.rvIndividu.visibility = View.VISIBLE
        }
    }


    // ---------------------------------------------
    // BACK & BERANDA BUTTON
    // ---------------------------------------------
    private fun setupNavigationButtons() {
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnBeranda.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

}