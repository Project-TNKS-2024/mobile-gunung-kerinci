package com.dicoding.gunungkerinci.Ticket

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.MainActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.Pembayaran.RincianPembayaranTiketActivity
import com.dicoding.gunungkerinci.databinding.ActivityTiketDataPendakiBinding
import com.dicoding.gunungkerinci.model.BookingData
import com.dicoding.gunungkerinci.model.BookingDetailData
import com.dicoding.gunungkerinci.model.PendakiFormulir
import com.dicoding.gunungkerinci.model.PendakiIdentityData
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch


class TiketDataPendakiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTiketDataPendakiBinding

    private lateinit var itemView: View

    private var bookingId = ""

    private var nomorPendaki = 1

    private var totalPendaki = 1

    private var namaPendaki = ""

    private var pendakiIdentity: PendakiIdentityData? = null

    private var bookingDetail: BookingDetailData? = null

    private var dataPendakiFormulir: List<PendakiFormulir> = emptyList()

    private val formLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {
            loadDataFormulir()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTiketDataPendakiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingId = intent.getStringExtra("booking_id") ?: ""

        nomorPendaki = intent.getIntExtra(
            "nomor_pendaki",
            1
        )

        totalPendaki = intent.getIntExtra(
            "total_pendaki",
            1
        )

        namaPendaki = intent.getStringExtra(
            "nama_pendaki"
        ) ?: ""

        loadDataFormulir()
        loadBookingDetail()

        Log.d("BOOKING_ID", bookingId)

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

        binding.btnSelanjutnya.setOnClickListener {
            // Cek apakah checkbox sudah dicentang
            if (!binding.checkBoxPersetujuan.isChecked) {
                Toast.makeText(
                    this,
                    "Harap setujui barang bawaan wajib terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Jika sudah dicentang → menuju halaman detail data pendakian
            val intent = Intent(this, RincianPembayaranTiketActivity::class.java)
            startActivity(intent)
        }

    }

    private fun loadBookingDetail() {
        lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(this@TiketDataPendakiActivity)
                    .getBookingDetail(bookingId)

                if (!response.isSuccessful) {
                    Toast.makeText(this@TiketDataPendakiActivity,
                        "Gagal mengambil data booking",
                        Toast.LENGTH_SHORT)
                        .show()
                    return@launch
                }

                val body = response.body()

                if (body?.success == true) {
                    bookingDetail = body.data
                    Log.d(
                        "BOOKING",
                        bookingDetail.toString()
                    )
                } else {
                    Toast.makeText(this@TiketDataPendakiActivity,
                        body?.message ?: "Data tidak ditemukan",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TiketDataPendakiActivity,
                    e.message,
                    Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun loadDataFormulir() {
        lifecycleScope.launch {
            try {
                val response = ApiConfig
                    .getApiService(this@TiketDataPendakiActivity)
                    .getDataFormulir(bookingId)

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@TiketDataPendakiActivity,
                        "Gagal mengambil data formulir",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val body = response.body()

                if (body?.success == true) {

                    // Simpan daftar pendaki yang sudah ada di booking
                    dataPendakiFormulir =
                        body.data.booking.pendakis

                    // Bangun ulang seluruh card pendaki
                    generatePendaki()

                } else {
                    Toast.makeText(
                        this@TiketDataPendakiActivity,
                        body?.message ?: "Data formulir tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@TiketDataPendakiActivity,
                    e.message ?: "Terjadi kesalahan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun generatePendaki() {
        binding.containerPendaki.removeAllViews()

        for (i in 1..totalPendaki) {

            val itemView = layoutInflater.inflate(
                R.layout.item_pendaki,
                binding.containerPendaki,
                false
            )

            val tvNamaPendaki =
                itemView.findViewById<TextView>(R.id.tvNamaPendaki)

            val tvIdPendaki =
                itemView.findViewById<TextView>(R.id.tvIdPendaki)

            val rbPendaki =
                itemView.findViewById<RadioButton>(R.id.rbPendaki)

            val btnIsiData =
                itemView.findViewById<Button>(R.id.btnIsiData)

            val btnUbahData =
                itemView.findViewById<Button>(R.id.btnUbahData)

            // Ambil data pendaki berdasarkan posisi card
            val pendaki = dataPendakiFormulir.getOrNull(i - 1)

            if (pendaki != null) {

                // SLOT SUDAH TERISI
                val biodata = pendaki.biodata

                tvNamaPendaki.text =
                    "${biodata.first_name.orEmpty()} ${biodata.last_name.orEmpty()}"
                        .trim()

                tvIdPendaki.text = pendaki.id_bio

                rbPendaki.visibility = View.VISIBLE
                btnIsiData.visibility = View.GONE
                btnUbahData.visibility = View.VISIBLE

                btnUbahData.setOnClickListener {
                    bukaFormPendaki(i)
                }

            } else {

                // SLOT BELUM TERISI
                tvNamaPendaki.text = "Pendaki $i"
                tvIdPendaki.text = "-"

                rbPendaki.visibility = View.GONE
                btnIsiData.visibility = View.VISIBLE
                btnUbahData.visibility = View.GONE

                btnIsiData.setOnClickListener {
                    bukaFormPendaki(i)
                }
            }

            binding.containerPendaki.addView(itemView)
        }
    }

    private fun bukaFormPendaki(nomor: Int) {
        val intent = Intent(
            this,
            TiketFormDataPendakiActivity::class.java
        )

        intent.putExtra(
            "booking_id",
            bookingId
        )

        intent.putExtra(
            "nomor_pendaki",
            nomor
        )

        intent.putExtra(
            "total_pendaki",
            totalPendaki
        )

        intent.putExtra(
            "nama_pendaki",
            "Pendaki $nomor"
        )

        formLauncher.launch(intent)
    }

    //Popup draft
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

    //Popup batalkan
    private fun showPopupBatalkan() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_pemesanan)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnBatal = dialog.findViewById<Button>(R.id.btnBatalPemesanan)
        val btnYakin = dialog.findViewById<Button>(R.id.btnYakinPemesanan)

        btnBatal.setOnClickListener {
            dialog.dismiss()
        }

        btnYakin.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Pemesanan tiket dibatalkan", Toast.LENGTH_SHORT).show()

            // kembali ke home fragment di main activity
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("navigate_home", true)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        dialog.show()
    }

    /*
    private fun tambahPendakiDefault() {
        // Inflate layout item_pendaki.xml ke dalam containerPendaki
        itemView = layoutInflater.inflate(
            R.layout.item_pendaki,
            binding.containerPendaki,
            false
        )

        // Ambil view di dalam item
        val tvNamaPendaki = itemView.findViewById<TextView>(R.id.tvNamaPendaki)
        val tvIdPendaki = itemView.findViewById<TextView>(R.id.tvIdPendaki)
        val btnIsiData = itemView.findViewById<Button>(R.id.btnIsiData)
        val rbPendaki = itemView.findViewById<RadioButton>(R.id.rbPendaki)
        val btnUbahData = itemView.findViewById<Button>(R.id.btnUbahData)


        // Set nilai default (untuk demo)
        tvNamaPendaki.text = "Pendaki 1"
        tvIdPendaki.text = "F12345678"
        rbPendaki.visibility = android.view.View.GONE
        btnUbahData.visibility = android.view.View.GONE


        // Aksi tombol "Isi Data" (sementara hanya toast, nanti bisa diarahkan ke form detail)
        btnIsiData.setOnClickListener {
            val intent = Intent(this, TiketFormDataPendakiActivity::class.java)
            // Jika kamu ingin kirim nama/id pendaki:
            intent.putExtra("nama_pendaki", tvNamaPendaki.text.toString())
            intent.putExtra("id_pendaki", tvIdPendaki.text.toString())
            formLauncher.launch(intent)
        }

        // Masukkan view ke container
        binding.containerPendaki.addView(itemView)
    }
     */

    private fun updatePendakiSelesai() {
        val rbPendaki = itemView.findViewById<RadioButton>(R.id.rbPendaki)
        val btnIsiData = itemView.findViewById<Button>(R.id.btnIsiData)
        val btnUbahData = itemView.findViewById<Button>(R.id.btnUbahData)

        rbPendaki.visibility = View.VISIBLE
        btnIsiData.visibility = View.GONE
        btnUbahData.visibility = View.VISIBLE

        // Klik ubah data kembali ke form
        btnUbahData.setOnClickListener {
            val intent = Intent(this, TiketFormDataPendakiActivity::class.java)
            formLauncher.launch(intent)
        }
    }
}