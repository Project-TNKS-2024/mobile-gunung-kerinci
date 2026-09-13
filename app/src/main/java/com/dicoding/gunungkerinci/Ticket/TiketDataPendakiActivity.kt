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
import com.dicoding.gunungkerinci.databinding.ActivityTiketDataPendakiBinding
import com.dicoding.gunungkerinci.model.BarangBawaan
import com.dicoding.gunungkerinci.model.BookingDetailData
import com.dicoding.gunungkerinci.model.FinalisasiFormulirRequest
import com.dicoding.gunungkerinci.model.FormulirItem
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

    private var selectedRadioButton: RadioButton? = null

    private var ketuaPendakiId = ""
    private var dataPendakiFormulir: List<PendakiFormulir> = emptyList()

    private val pendakiSudahIsiDarurat = mutableSetOf<String>()

    private val formLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == RESULT_OK) {
            val nomorPendakiSelesai = result.data?.getIntExtra("nomor_pendaki", -1) ?: -1

            if (nomorPendakiSelesai == -1) {
                val pendaki = dataPendakiFormulir.getOrNull(nomorPendakiSelesai - 1)
                if (pendaki != null) {
                    pendakiSudahIsiDarurat.add(pendaki.id)

                    Log.d(
                        "STATUS_DARURAT_BOOKING",
                        "Pendaki $nomorPendakiSelesai |" +
                        "id_bio=${pendaki.id_bio} |" +
                        "sudah isi nomor darurat"
                    )
                }
            }
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

        loadBookingDetail()

        Log.d("BOOKING_ID", bookingId)

        // Tombol back di header
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Tombol Batalkan → tampil popup
        binding.btnBatalkan.setOnClickListener {
            Log.d("TEST_BATALKAN", "BUTTON BATALKAN DIKLIK")

            showPopupBatalkan()
        }

        // Tombol Simpan Draft → tampil popup draft
        binding.btnSimpanDraft.setOnClickListener {
            showPopupDraft()
        }

        binding.btnSelanjutnya.setOnClickListener {
            if (ketuaPendakiId.isEmpty()) {
                Toast.makeText(
                    this,
                    "Silakan pilih Ketua Pendakian terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Cek apakah checkbox sudah dicentang
            if (!binding.checkBoxPersetujuan.isChecked) {
                Toast.makeText(
                    this,
                    "Harap setujui barang bawaan wajib terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            finalisasiFormulir()
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
                    loadDataFormulir()
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

        // RadioButton hanya boleh muncul jika SEMUA pendaki sudah memiliki data
        val semuaPendakiSudahDiisi =
            dataPendakiFormulir.size >= totalPendaki

        Log.d(
            "STATUS_PENDAKI",
            "Total pendaki: $totalPendaki | " +
                    "Pendaki terisi: ${dataPendakiFormulir.size} | " +
                    "Semua terisi: $semuaPendakiSudahDiisi"
        )

        for (i in 0 until totalPendaki) {
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
            val pendaki = dataPendakiFormulir.getOrNull(i)

            if (pendaki != null) {
                val biodata = pendaki.biodata

                tvNamaPendaki.text =
                    "${biodata.first_name.orEmpty()} ${biodata.last_name.orEmpty()}".trim()

                tvIdPendaki.text = pendaki.id_bio
                btnIsiData.visibility = View.GONE
                btnUbahData.visibility = View.VISIBLE

                // =====================================================
                // RADIO BUTTON
                // =====================================================
                // RadioButton hanya muncul jika SEMUA pendaki yang sudah selesai diinput
                if (semuaPendakiSudahDiisi) {
                     rbPendaki.visibility = View.VISIBLE

                    //Jika sebelumnya pendaki ini sudah dipilih sebagai ketua, pertahankan pilihannya
                    if (ketuaPendakiId == pendaki.id) {
                        rbPendaki.isChecked = true
                        selectedRadioButton = rbPendaki
                    }

                    rbPendaki.setOnClickListener {

                        // Hilangkan pilihan sebelumnya
                        selectedRadioButton?.isChecked = false

                        // Simpan RadioButton yang baru atau sekarang dipilih
                        selectedRadioButton = rbPendaki

                        // Centang RadioButton yang dipilih sekarang
                        rbPendaki.isChecked = true

                        // Simpan ID pendaki sebagai ketua
                        ketuaPendakiId = pendaki.id

                        Log.d(
                            "KETUA_PENDAKIAN",
                            "Ketua dipilih: $ketuaPendakiId"
                        )
                    }
                } else {
                    //Belum semua pendaki terisi, jadi RadioButton belum boleh muncul
                    rbPendaki.visibility = View.GONE
                    rbPendaki.isChecked = false
                }

                btnUbahData.setOnClickListener {
                    bukaFormPendaki(
                        nomor = i + 1,
                        modeEdit = true
                    )
                }
            } else {
                // =====================================================
                // PENDAKI BELUM DIISI
                // =====================================================
                tvNamaPendaki.text = "Pendaki ${i + 1}"
                tvIdPendaki.text = "-"

                // RadioButton tidak boleh muncul
                rbPendaki.visibility = View.GONE
                rbPendaki.isChecked = false

                btnIsiData.visibility = View.VISIBLE
                btnUbahData.visibility = View.GONE

                btnIsiData.setOnClickListener {
                    // Cek apakah pendaki sebelumnya sudah selesai diisi
                    if (!bolehIsiPendaki(i)) {
                        Toast.makeText(
                            this,
                            "Silakan isi dan simpan Data Pendaki ${i} terlebih dahulu",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    // Jika urutan sudah benar, buka form
                    bukaFormPendaki(
                        nomor = i + 1,
                        modeEdit = false
                    )
                }
            }
            binding.containerPendaki.addView(itemView)
        }
    }

    private fun bolehIsiPendaki(index: Int): Boolean {

        // Pendaki 1 selalu boleh dibuka
        if (index == 0) {
            return true
        }

        // Semua pendaki sebelumnya harus sudah ada
        // dan nomor daruratnya sudah tersimpan
        for (i in 0 until index) {
            val pendakiSebelumnya =
                dataPendakiFormulir.getOrNull(i)
                    ?: return false

            val idPendaki =
                pendakiSebelumnya.id_bio.orEmpty()

            val noHpDarurat =
                pendakiSebelumnya.biodata.no_hp_darurat.orEmpty()

            if (idPendaki.isBlank()) {
                return false
            }
        }
        return true
    }

    private fun bukaFormPendaki(nomor: Int, modeEdit: Boolean) {
        val intent = Intent(
            this,
            TiketFormDataPendakiActivity::class.java
        )

        intent.putExtra("booking_id",bookingId)
        intent.putExtra("mode_edit", modeEdit)
        intent.putExtra("nomor_pendaki",nomor)
        intent.putExtra("total_pendaki",totalPendaki)
        intent.putExtra("nama_pendaki","Pendaki $nomor")
        formLauncher.launch(intent)
    }

    private fun finalisasiFormulir() {
        val formulir = dataPendakiFormulir.map { pendaki ->
            FormulirItem(
                id_pendaki = pendaki.id,
                kode_bio = pendaki.id_bio,
                no_hp_darurat = pendaki.biodata.no_hp_darurat.orEmpty()
            )
        }

        val barangBawaan = listOf(
            BarangBawaan(
                nama_barang = "Perlengkapan standar pendakian gunung",
                jumlah = 1
            ),
            BarangBawaan(
                nama_barang = "Trash Bag",
                jumlah = 1
            ),
            BarangBawaan(
                nama_barang = "Survival kit standar",
                jumlah = 1
            ),
            BarangBawaan(
                nama_barang = "P3K Standar",
                jumlah = 1
            )
        )

        val request = FinalisasiFormulirRequest(
            id_booking = bookingId,
            action = "next",
            barangWajib = true,
            formulir = formulir,
            barang_bawaan = barangBawaan
        )

        lifecycleScope.launch {
            try {
                val response = ApiConfig
                    .getApiService(this@TiketDataPendakiActivity)
                    .finalisasiFormulir(request)
                Log.d("FINALISASI_REQUEST", request.toString())

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@TiketDataPendakiActivity,
                        "Gagal melakukan finalisasi formulir",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val body = response.body()
                Log.d("FINALISASI_RESPONSE", body.toString())

                if (body?.success == true) {
                    val intent = Intent(this@TiketDataPendakiActivity,
                        RincianPemesananTiketActivity::class.java)
                    intent.putExtra("booking_id", bookingId)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@TiketDataPendakiActivity,
                        body?.message ?: "Finalisasi formulir gagal",
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
        Log.d("TEST_BATALKAN", "MASUK showPopupBatalkan()")

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_pemesanan)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnBatal = dialog.findViewById<Button>(R.id.btnBatalPemesanan)
        val btnYakin = dialog.findViewById<Button>(R.id.btnYakinPemesanan)

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

        Log.d("TEST_BATALKAN", "DIALOG POPUP SUDAH DI-SHOW")
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

                val response = ApiConfig
                    .getApiService(this@TiketDataPendakiActivity)
                    .cancelBooking(bookingId)

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
                        this@TiketDataPendakiActivity,
                        "Gagal membatalkan pemesanan. HTTP ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val body = response.body()

                if (body?.success == true) {

                    Toast.makeText(
                        this@TiketDataPendakiActivity,
                        body.message ?: "Pemesanan berhasil dibatalkan",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Kembali ke Home
                    val intent = Intent(
                        this@TiketDataPendakiActivity,
                        MainActivity::class.java
                    )

                    intent.putExtra("navigate_home", true)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                    startActivity(intent)
                    finish()

                } else {

                    Toast.makeText(
                        this@TiketDataPendakiActivity,
                        body?.message ?: "Pemesanan gagal dibatalkan",
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
                    this@TiketDataPendakiActivity,
                    e.message ?: "Terjadi kesalahan saat membatalkan pemesanan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}