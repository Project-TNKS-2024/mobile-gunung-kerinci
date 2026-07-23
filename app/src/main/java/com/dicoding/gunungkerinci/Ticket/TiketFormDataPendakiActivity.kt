package com.dicoding.gunungkerinci.Ticket

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.dicoding.gunungkerinci.databinding.ActivityTiketFormDataPendakiBinding
import com.dicoding.gunungkerinci.model.TambahPendakiRequest
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import com.dicoding.gunungkerinci.model.PendakiFormulir
import com.dicoding.gunungkerinci.model.UpdatePendakiRequest
import com.dicoding.gunungkerinci.model.SimpanFormulirRequest
import com.dicoding.gunungkerinci.model.FormulirItem

class TiketFormDataPendakiActivity : AppCompatActivity() {

    private var bookingId = ""

    private var nomorPendaki = 1

    private var totalPendaki = 1

    private var pendakiId = ""

    private var kodeBio = ""

    private lateinit var binding: ActivityTiketFormDataPendakiBinding

    private val dataFormPendaki = mutableListOf<DataFormPendakiSementara>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTiketFormDataPendakiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingId = intent.getStringExtra("booking_id") ?: ""

        nomorPendaki = intent.getIntExtra("nomor_pendaki", 1)
        totalPendaki = intent.getIntExtra("total_pendaki", 1)

        repeat(totalPendaki) {
            dataFormPendaki.add(DataFormPendakiSementara())
        }

        tampilkanPendaki()

        // Terima nama pendaki dari halaman sebelumnya
        val namaPendaki = intent.getStringExtra("nama_pendaki") ?: "Pendaki 1"
        binding.textIsiDataPendakian.text = namaPendaki

        // Tombol Back
        binding.buttonBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // BUTTON CARI Pendaki
        binding.btnCariPendaki.setOnClickListener {
            val code = binding.inputIdPendaki.text.toString().trim()

            if (code.isEmpty()) {
                Toast.makeText(
                    this,
                    "Masukkan ID Pendaki",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (bookingId.isEmpty()) {
                Toast.makeText(
                    this,
                    "Booking ID tidak ditemukan",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }
            //tambahPendaki(code)
            prosesCariPendaki(code)
        }

        binding.btnNext.setOnClickListener {
            //Wajib mengisi nomor darurat
            if (!validasiFormPendaki()) {
                return@setOnClickListener
            }

            //Simpan semua data pendaki yang sedang tampil
            simpanDataFormSaatIni()

            if (nomorPendaki < totalPendaki) {
                nomorPendaki++
                tampilkanPendaki()
            }

        }

        binding.btnPrev.setOnClickListener {
            //Simpan kondisi form saat ini sebelum kembali
            simpanDataFormSaatIni()

            if (nomorPendaki > 1) {
                nomorPendaki--
                tampilkanPendaki()
            }
        }

        binding.buttonSimpan.setOnClickListener {
            if (!validasiFormPendaki()) {
                return@setOnClickListener
            }

            //Simpan data pendaki terakhir ke penyimpanan sementara
            simpanDataFormSaatIni()

            // Periksa seluruh pendaki dari Pendaki 1 sampai terakhir
            if (!validasiSemuaPendaki()) {
                return@setOnClickListener
            }

            //Simpan data formulit pendaki ke backend
            simpanFormulir()
        }

    }

    private fun validasiSemuaPendaki(): Boolean {
        dataFormPendaki.forEachIndexed { index, data ->
            val nomor = index + 1

            // Pastikan pendaki sudah berhasil ditambahkan ke booking
            if (data.idPendaki.isEmpty() || data.kodeBio.isEmpty()) {
                Toast.makeText(
                    this,
                    "Data Pendaki $nomor belum dilengkapi",
                    Toast.LENGTH_SHORT
                ).show()

                nomorPendaki = nomor
                tampilkanPendaki()
                return false
            }

            // Pastikan ID yang tampil memang ID yang berhasil diproses
            if (data.idPendakiInput != data.kodeBio) {
                Toast.makeText(
                    this,
                    "ID Pendaki $nomor belum dikonfirmasi. Klik Cari terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()

                nomorPendaki = nomor
                tampilkanPendaki()
                return false
            }

            // Pastikan nomor telepon darurat sudah diisi
            if (data.noHpDarurat.isEmpty()) {
                Toast.makeText(
                    this,
                    "Nomor telepon darurat Pendaki $nomor belum diisi",
                    Toast.LENGTH_SHORT
                ).show()

                nomorPendaki = nomor
                tampilkanPendaki()

                binding.inputTelDarurat.error =
                    "Nomor telepon darurat wajib diisi"

                binding.inputTelDarurat.requestFocus()
                return false
            }
        }
        return true
    }

    private fun prosesCariPendaki(code: String) {
        val index = nomorPendaki - 1

        if (index !in dataFormPendaki.indices) {
            return
        }

        val dataSaatIni = dataFormPendaki[index]

        // Cek apakah ID sudah digunakan oleh slot pendaki lain
        val idSudahDigunakan = dataFormPendaki.withIndex().any() {
                (posisi, data) -> posisi != index && data.kodeBio == code
        }

        if (idSudahDigunakan) {
            Toast.makeText(
                this,
                "ID Pendaki sudah digunakan oleh anggota lain",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        when {
            // Slot ini belum pernah memiliki pendaki
            dataSaatIni.idPendaki.isEmpty() -> {
                tambahPendaki(code)
            }

            // Slot sudah punya pendaki dan ID yang dicari masih sama
            dataSaatIni.kodeBio == code -> {
                Toast.makeText(
                    this,
                    "Data pendaki sudah ditampilkan",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Slot sudah punya pendaki, tetapi ID diganti
            else -> {
                updatePendaki(
                    idPendaki = dataSaatIni.idPendaki,
                    codeBaru = code
                )
            }
        }
    }
    private fun updatePendaki(idPendaki: String, codeBaru: String) {
        lifecycleScope.launch {
            try {
                val request = UpdatePendakiRequest (
                    booking = bookingId,
                    id = idPendaki,
                    code = codeBaru
                )

                val response = ApiConfig
                    .getApiService(this@TiketFormDataPendakiActivity)
                    .updatePendaki(request)

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body?.success == true) {
                        Toast.makeText(
                            this@TiketFormDataPendakiActivity,
                            body.message,
                            Toast.LENGTH_SHORT
                        ).show()

                        //Setelah behasil update, ambil kembali biodata pendaki yang baru
                        ambilDataPendaki(codeBaru)
                    } else {
                        Toast.makeText(
                            this@TiketFormDataPendakiActivity,
                            body?.message ?: "Gagal memperbarui pendaki",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@TiketFormDataPendakiActivity,
                        "Gagal memperbarui data pendaki",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TiketFormDataPendakiActivity,
                    e.message ?: "Terjadi kesalahan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //Validasi yang digunakan untuk tombol Selanjutnya dan Simpan
    private fun validasiFormPendaki(): Boolean {
        val idPendakiInput =
            binding.inputIdPendaki.text.toString().trim()

        //Id belum diisi
        if (idPendakiInput.isEmpty()) {
            binding.inputIdPendaki.error = "ID Pendaki wajib diisi"
            binding.inputIdPendaki.requestFocus()
            return false
        }

        //Belum perbah berhasil Tambah Pendaki
        if (pendakiId.isEmpty() || kodeBio.isEmpty()) {
            binding.inputIdPendaki.error = "Klik Cari terlebih dahulu"
            binding.inputIdPendaki.requestFocus()
            return false
        }

        //ID di input berbeda dengan ID yang terakhir berhasil disimpan melalui Tambah/Update
        if (idPendakiInput != kodeBio) {
            binding.inputIdPendaki.error = "ID Pendaki berubah. Klik Cari terlebih dahulu"
            binding.inputIdPendaki.requestFocus()
            return false
        }

        binding.inputIdPendaki.error = null
        val noHpDarurat =
            binding.inputTelDarurat.text.toString().trim()

        if (noHpDarurat.isEmpty()) {
            binding.inputTelDarurat.error =
                "Nomor telepon darurat wajib diisi"
            return false
        }

        binding.inputTelDarurat.error = null
        return true
    }

    private fun simpanFormulir() {
        lifecycleScope.launch {
            try {
                val formulir = dataFormPendaki.map { data ->
                    FormulirItem(
                        id_pendaki = data.idPendaki,
                        kode_bio = data.kodeBio,
                        no_hp_darurat = data.noHpDarurat
                    )
                }

                val request = SimpanFormulirRequest(
                    id_booking = bookingId,
                    action = "save",
                    formulir = formulir,

                    // Barang bawaan belum diproses di halaman ini
                    barang_bawaan = emptyList()
                )

                val response = ApiConfig
                    .getApiService(this@TiketFormDataPendakiActivity)
                    .simpanFormulir(request)

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body?.success == true) {
                        Toast.makeText(
                            this@TiketFormDataPendakiActivity,
                            body.message ?: "Data pendaki berhasil disimpan",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Kembali ke halaman Data Pendaki
                        setResult(RESULT_OK)
                        finish()

                    } else {
                        Toast.makeText(
                            this@TiketFormDataPendakiActivity,
                            body?.message ?: "Gagal menyimpan data pendaki",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    Toast.makeText(
                        this@TiketFormDataPendakiActivity,
                        "Gagal menyimpan data pendaki",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@TiketFormDataPendakiActivity,
                    e.message ?: "Terjadi kesalahan saat menyimpan data",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun simpanDataFormSaatIni() {

        val index = nomorPendaki - 1

        if (index !in dataFormPendaki.indices) {
            return
        }

        dataFormPendaki[index] = DataFormPendakiSementara(
            idPendaki = pendakiId,
            kodeBio = kodeBio,
            idPendakiInput =
                binding.inputIdPendaki.text.toString().trim(),

            namaDepan =
                binding.inputNamaDepan.text.toString().trim(),

            namaBelakang =
                binding.inputNamaBelakang.text.toString().trim(),

            negara =
                binding.inputNegara.text.toString().trim(),

            tanggalLahir =
                binding.inputTglLahir.text.toString().trim(),

            usia =
                binding.inputUsia.text.toString().trim(),

            noTelepon =
                binding.inputNoTelepon.text.toString().trim(),

            noHpDarurat =
                binding.inputTelDarurat.text.toString().trim()
        )
    }

    private fun tampilkanPendaki() {
        val index = nomorPendaki - 1

        if (index !in dataFormPendaki.indices) {
            return
        }

        val data = dataFormPendaki[index]

        binding.textIsiDataPendakian.text =
            "Isi Data Pendaki $nomorPendaki"

        binding.inputIdPendaki.setText(data.idPendakiInput)
        binding.inputNamaDepan.setText(data.namaDepan)
        binding.inputNamaBelakang.setText(data.namaBelakang)
        binding.inputNegara.setText(data.negara)
        binding.inputTglLahir.setText(data.tanggalLahir)
        binding.inputUsia.setText(data.usia)
        binding.inputNoTelepon.setText(data.noTelepon)
        binding.inputTelDarurat.setText(data.noHpDarurat)

        pendakiId = data.idPendaki
        kodeBio = data.kodeBio

        aturTombolNavigasi()
    }

    private fun aturTombolNavigasi() {
        binding.btnPrev.visibility = if (nomorPendaki > 1) {
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.btnNext.visibility = if (nomorPendaki < totalPendaki) {
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.buttonSimpan.visibility = if (nomorPendaki == totalPendaki) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun tambahPendaki(code: String) {
        lifecycleScope.launch {
            try {
                val request = TambahPendakiRequest(
                    booking = bookingId,
                    code = code
                )

                val response = ApiConfig
                    .getApiService(this@TiketFormDataPendakiActivity)
                    .tambahPendaki(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        Toast.makeText(
                            this@TiketFormDataPendakiActivity,
                            body.message ?: "Pendaki berhasil ditemukan",
                            Toast.LENGTH_SHORT
                        ).show()

                        ambilDataPendaki(code)
                    } else {
                        Toast.makeText(
                            this@TiketFormDataPendakiActivity,
                            body?.message ?: "Pendaki tidak ditemukan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@TiketFormDataPendakiActivity,
                        "Pendaki tidak ditemukan atau tidak dapat ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TiketFormDataPendakiActivity,
                    e.message ?: "Terjadi kesalahan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun ambilDataPendaki(code: String) {
        lifecycleScope.launch {
            try {
                val response = ApiConfig
                    .getApiService(this@TiketFormDataPendakiActivity)
                    .getDataFormulir(bookingId)

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@TiketFormDataPendakiActivity,
                        "Gagal mengambil data pendaki",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val body = response.body()

                if (body?.success == true) {
                    val pendaki = body.data.booking.pendakis.find {
                        it.id_bio == code
                    }

                    if (pendaki != null) {
                        isiDataPendaki(pendaki)
                    } else {
                        Toast.makeText(
                            this@TiketFormDataPendakiActivity,
                            "Data pendaki tidak ditemukan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@TiketFormDataPendakiActivity,
                        body?.message ?: "Gaga; mengambil data pendaki",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@TiketFormDataPendakiActivity,
                    e.message ?: "Terjadi kesalahan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isiDataPendaki(pendaki: PendakiFormulir) {
        val biodata = pendaki.biodata

        pendakiId = pendaki.id
        kodeBio = pendaki.id_bio

        binding.inputIdPendaki.setText(pendaki.id_bio)
        binding.inputNamaDepan.setText(biodata.first_name.orEmpty())
        binding.inputNamaBelakang.setText(biodata.last_name.orEmpty())
        binding.inputNegara.setText(biodata.dataNegara?.name.orEmpty())

        binding.inputTglLahir.setText(
            biodata.tanggal_lahir
                ?.substringBefore("T")
                .orEmpty()
        )

        binding.inputUsia.setText(pendaki.usia.toString())
        binding.inputNoTelepon.setText(biodata.no_hp.orEmpty())

        // Nomor darurat tetap diisi manual
        binding.inputTelDarurat.setText("")
        binding.inputTelDarurat.error = null

        //Simpan biodata hasil pencarian ke slot pendaki yang sedang aktif
        simpanDataFormSaatIni()
    }
}

data class DataFormPendakiSementara(
    var idPendaki: String = "",
    var kodeBio: String = "",
    var idPendakiInput: String = "",
    var namaDepan: String = "",
    var namaBelakang: String = "",
    var negara: String = "",
    var tanggalLahir: String = "",
    var usia: String = "",
    var noTelepon: String = "",
    var noHpDarurat: String = ""
)