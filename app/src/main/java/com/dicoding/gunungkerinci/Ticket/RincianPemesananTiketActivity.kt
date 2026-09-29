package com.dicoding.gunungkerinci.Ticket

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.DataPendaki.DetailDataPendakiActivity
import com.dicoding.gunungkerinci.Ticket.Pembayaran.PembayaranActivity
import com.dicoding.gunungkerinci.databinding.ActivityRincianPemesananTiketBinding
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.model.BookingDetailData
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class RincianPemesananTiketActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRincianPemesananTiketBinding

    private var bookingId = ""
    private var ketuaPendakiId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRincianPemesananTiketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingId = intent.getStringExtra("booking_id").orEmpty()

        ketuaPendakiId =
            intent.getStringExtra("ketua_pendaki_id").orEmpty()

        Log.d("RINCIAN_BOOKING", "Booking ID: $bookingId")

        if (bookingId.isNotBlank()) {
            getBookingDetail()
        } else {
            Toast.makeText(
                this,
                "Booking ID tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Tombol back di header
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Tombol Batalkan → tampil popup
        binding.btnBatalkan.setOnClickListener {
            showPopupBatalkan()
        }

        // Tombol Simpan Draft → tampil popup draft
        binding.btnSimpanDraft.setOnClickListener {
            showPopupDraft()
        }

        binding.btnBayar.setOnClickListener{
            val intent = Intent(this, PembayaranActivity::class.java)
            intent.putExtra(
                "booking_id",
                bookingId
            )
            startActivity(intent)
        }

        binding.lytDetailPendaki.setOnClickListener {
            bukaDetailPendaki()
        }
    }

    private fun getBookingDetail() {
        lifecycleScope.launch {
            try {

                Log.d(
                    "RINCIAN_API",
                    "Memanggil getBookingDetail dengan booking_id: $bookingId"
                )

                val response = ApiConfig
                    .getApiService(this@RincianPemesananTiketActivity)
                    .getBookingDetail(bookingId)

                Log.d(
                    "RINCIAN_API",
                    "HTTP Code: ${response.code()}"
                )

                if (response.isSuccessful) {

                    val body = response.body()

                    Log.d(
                        "RINCIAN_API",
                        "Response: $body"
                    )

                    if (body?.success == true) {

                        tampilkanDataBooking(body.data)

                    } else {

                        Toast.makeText(
                            this@RincianPemesananTiketActivity,
                            body?.message ?: "Data booking tidak ditemukan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    Log.e(
                        "RINCIAN_API",
                        "Error ${response.code()}: ${response.errorBody()?.string()}"
                    )

                    Toast.makeText(
                        this@RincianPemesananTiketActivity,
                        "Gagal mengambil data pemesanan",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "RINCIAN_API",
                    "Exception: ${e.message}",
                    e
                )

                Toast.makeText(
                    this@RincianPemesananTiketActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun tampilkanDataBooking(data: BookingDetailData) {

        // =========================
        // RINCIAN PEMESANAN
        // =========================

        binding.tvJumlahAnggota.text =
            "${data.pendakis.size} Orang"

        binding.tvGerbangMasuk.text =
            data.gate_masuk.nama

        binding.tvGerbangKeluar.text =
            data.gate_keluar.nama

        binding.tvTanggalMasuk.text =
            formatTanggal(data.tanggal_masuk)

        binding.tvTanggalKeluar.text =
            formatTanggal(data.tanggal_keluar)


        // =========================
        // NAMA PEMESAN
        // =========================

        try {
            val dataStruk = data.dataStruk

            if (!dataStruk.isNullOrBlank()) {

                val json = org.json.JSONObject(dataStruk)
                val user = json.optJSONObject("user")
                val biodata = user?.optJSONObject("biodata")

                val namaPemesan =
                    biodata?.optString("fullName").orEmpty()

                if (namaPemesan.isNotBlank()) {
                    binding.tvNamaPemesan.text = namaPemesan
                }
            }

        } catch (e: Exception) {
            Log.e(
                "RINCIAN_NAMA",
                "Gagal mengambil nama pemesan: ${e.message}",
                e
            )
        }


        // =========================
        // KETUA & ANGGOTA PENDAKI
        // =========================

        if (data.pendakis.isNotEmpty()) {

            // =========================
            // KETUA
            // =========================

            val ketua = data.pendakis.find {
                it.id == ketuaPendakiId
            }

            val namaKetua = ketua?.let {
                "${it.biodata.first_name.orEmpty()} " +
                        it.biodata.last_name.orEmpty()
            }?.trim().orEmpty()

            binding.tvKetua.text =
                if (namaKetua.isNotBlank()) namaKetua else "-"


            // =========================
            // ANGGOTA
            // =========================

            tampilkanAnggota(data)


            // =========================
            // JUMLAH WNI & WNA
            // =========================

            val totalWNI = data.pendakis.count {
                it.biodata.dataNegara?.code == "ID"
            }

            val totalWNA = data.pendakis.count {
                it.biodata.dataNegara?.code != null &&
                        it.biodata.dataNegara?.code != "ID"
            }

            binding.ttlWNI.text =
                "$totalWNI Pendaki"

            binding.ttlWNA.text =
                "$totalWNA Pendaki"


            // =========================
            // HARGA WNI & WNA
            // =========================

            val hargaWNI = data.gktiket?.tiket_pendaki
                ?.firstOrNull {
                    it.kategori_pendaki.equals(
                        "wni",
                        ignoreCase = true
                    )
                }

            val hargaWNA = data.gktiket?.tiket_pendaki
                ?.firstOrNull {
                    it.kategori_pendaki.equals(
                        "wna",
                        ignoreCase = true
                    )
                }


            // =========================
            // HARGA MASUK
            // =========================
            val hargaMasukWNI =
                hargaWNI?.harga_masuk_wk ?: 0

            val hargaMasukWNA =
                hargaWNA?.harga_masuk_wk ?: 0


            // =========================
            // HARGA BERKEMAH
            // =========================
            val hargaBerkemahWNI =
                hargaWNI?.harga_kemah ?: 0

            val hargaBerkemahWNA =
                hargaWNA?.harga_kemah ?: 0


            // =========================
            // HARGA HIKING / CLIMBING
            // =========================
            val hargaHikingWNI =
                hargaWNI?.harga_traking ?: 0

            val hargaHikingWNA =
                hargaWNA?.harga_traking ?: 0


            // =========================
            // RINCIAN HARGA WNI
            // =========================
            binding.ttlMasukWNI.text =
                formatAngka(
                    hargaMasukWNI * totalWNI
                )

            binding.ttlBerkemahWNI.text =
                formatAngka(
                    hargaBerkemahWNI * totalWNI
                )

            binding.ttlHiCliWNI.text =
                formatAngka(
                    hargaHikingWNI * totalWNI
                )


            // =========================
            // RINCIAN HARGA WNA
            // =========================
            if (totalWNA > 0) {

                binding.ttlMasukWNA.text =
                    formatAngka(
                        hargaMasukWNA * totalWNA
                    )

                binding.ttlBerkemahWNA.text =
                    formatAngka(
                        hargaBerkemahWNA * totalWNA
                    )

                binding.ttlHiCliWNA.text =
                    formatAngka(
                        hargaHikingWNA * totalWNA
                    )

            } else {

                binding.ttlMasukWNA.text =
                    "Rp 0"

                binding.ttlBerkemahWNA.text =
                    "Rp 0"

                binding.ttlHiCliWNA.text =
                    "Rp 0"
            }


            // =========================
            // TOTAL TAGIHAN WNI & WNA
            // =========================
            val totalTagihanWNI = data.pendakis
                .filter {
                    it.biodata.dataNegara?.code == "ID"
                }
                .mapNotNull {
                    it.tagihan
                }
                .sum()

            val totalTagihanWNA = data.pendakis
                .filter {
                    it.biodata.dataNegara?.code != null &&
                            it.biodata.dataNegara?.code != "ID"
                }
                .mapNotNull {
                    it.tagihan
                }
                .sum()


            binding.ttlHargaWNI.text =
                formatRupiah(totalTagihanWNI)

            binding.ttlHargaWNA.text =
                formatRupiah(totalTagihanWNA)


            // =========================
            // TOTAL PEMBAYARAN
            // =========================
            binding.ttlPembayaran.text =
                formatRupiah(data.total_pembayaran)
        }


        // =========================
        // HARI & MALAM
        // =========================
        binding.ttlHari.text =
            "${data.total_hari} Hari"

        binding.ttlMalam.text =
            "${if (data.total_hari > 0) data.total_hari - 1 else 0} Malam"
    }

    private fun tampilkanAnggota(data: BookingDetailData) {

        val anggota = data.pendakis.filter {
            it.id != ketuaPendakiId
        }

        binding.lytListPendaki.removeAllViews()


        // =========================
        // TIDAK ADA ANGGOTA
        // =========================

        if (anggota.isEmpty()) {
            binding.tvLihatDetail.visibility =
                android.view.View.GONE

            return
        }


        // =========================
        // TAMPILKAN MAKSIMAL 2 NAMA
        // =========================

        val jumlahDitampilkan =
            minOf(anggota.size, 2)

        for (i in 0 until jumlahDitampilkan) {

            val pendaki = anggota[i]

            val namaPendaki =
                listOfNotNull(
                    pendaki.biodata.first_name,
                    pendaki.biodata.last_name
                ).joinToString(" ")

            val textView =
                TextView(this)

            textView.text =
                if (namaPendaki.isNotBlank()) {
                    namaPendaki
                } else {
                    "-"
                }

            textView.textSize = 16f

            textView.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    this,
                    R.color.abutua
                )
            )

            binding.lytListPendaki.addView(
                textView
            )
        }


        // =========================
        // JIKA ANGGOTA LEBIH DARI 2
        // =========================

        if (anggota.size > 2) {

            val textView =
                TextView(this)

            textView.text =
                "..."

            textView.textSize = 16f

            textView.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    this,
                    R.color.abutua
                )
            )

            binding.lytListPendaki.addView(
                textView
            )
        }


        // =========================
        // LIHAT DETAIL
        // =========================

        binding.tvLihatDetail.visibility =
            android.view.View.VISIBLE

        binding.tvLihatDetail.setOnClickListener {
            bukaDetailPendaki()
        }
    }

    private fun bukaDetailPendaki() {

        val intent = Intent(
            this,
            DetailDataPendakiActivity::class.java
        )

        intent.putExtra(
            "booking_id",
            bookingId
        )

        intent.putExtra(
            "ketua_pendaki_id",
            ketuaPendakiId
        )

        startActivity(intent)
    }

    private fun formatRupiah(nominal: Int): String {
        return "Rp ${String.format("%,d", nominal).replace(",", ".")}"
    }

    private fun formatAngka(nominal: Int): String {
        return String.format(
            Locale.getDefault(),
            "%,d",
            nominal
        ).replace(",", ".")
    }

    private fun formatTanggal(tanggal: String): String {
        return try {
            val inputFormat = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

            val outputFormat = SimpleDateFormat(
                "dd MMMM yyyy",
                Locale("id", "ID")
            )

            val date = inputFormat.parse(tanggal)

            outputFormat.format(date!!)
        } catch (e: Exception) {
            tanggal
        }
    }

    private fun showPopupDraft() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_draft)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnBatal = dialog.findViewById<Button>(R.id.btnBatalDraft)
        val btnYakin = dialog.findViewById<Button>(R.id.btnYakinDraft)

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnYakin.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Draft pemesanan tiket berhasil disimpan", Toast.LENGTH_SHORT).show()

            // kembali ke home
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    private fun showPopupBatalkan() {

        Log.d(
            "TEST_BATALKAN",
            "MASUK showPopupBatalkan()"
        )

        val dialog = Dialog(this)

        dialog.setContentView(
            R.layout.popup_pemesanan
        )

        dialog.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        val btnBatal =
            dialog.findViewById<Button>(
                R.id.btnBatalPemesanan
            )

        val btnYakin =
            dialog.findViewById<Button>(
                R.id.btnYakinPemesanan
            )

        btnBatal.setOnClickListener {

            dialog.dismiss()
        }

        btnYakin.setOnClickListener {

            Log.d(
                "TEST_BATALKAN",
                "BUTTON YAKIN DIKLIK"
            )

            dialog.dismiss()

            cancelBooking()
        }

        dialog.show()

        Log.d(
            "TEST_BATALKAN",
            "DIALOG POPUP SUDAH DI-SHOW"
        )
    }

    private fun cancelBooking() {

        if (bookingId.isBlank()) {

            Toast.makeText(
                this,
                "Booking ID tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                Log.d(
                    "CANCEL_BOOKING",
                    "Membatalkan booking: $bookingId"
                )

                val response =
                    ApiConfig
                        .getApiService(
                            this@RincianPemesananTiketActivity
                        )
                        .cancelBooking(
                            bookingId
                        )

                Log.d(
                    "CANCEL_BOOKING",
                    "HTTP: ${response.code()}"
                )

                Log.d(
                    "CANCEL_BOOKING",
                    "Response: ${response.body()}"
                )

                if (!response.isSuccessful) {

                    Toast.makeText(
                        this@RincianPemesananTiketActivity,
                        "Gagal membatalkan pemesanan. HTTP ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val body =
                    response.body()

                if (body?.success == true) {

                    Toast.makeText(
                        this@RincianPemesananTiketActivity,
                        body.message
                            ?: "Pemesanan berhasil dibatalkan",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent =
                        Intent(
                            this@RincianPemesananTiketActivity,
                            MainActivity::class.java
                        )

                    intent.putExtra(
                        "navigate_home",
                        true
                    )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP

                    startActivity(intent)

                    finish()

                } else {

                    Toast.makeText(
                        this@RincianPemesananTiketActivity,
                        body?.message
                            ?: "Pemesanan gagal dibatalkan",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "CANCEL_BOOKING",
                    "Error cancel booking",
                    e
                )

                Toast.makeText(
                    this@RincianPemesananTiketActivity,
                    e.message
                        ?: "Terjadi kesalahan saat membatalkan pemesanan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}