package com.dicoding.gunungkerinci.Ticket

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.dicoding.gunungkerinci.databinding.ActivityTiketFormDataPendakiBinding
import com.dicoding.gunungkerinci.model.TambahPendakiRequest
import androidx.lifecycle.lifecycleScope
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch
import com.dicoding.gunungkerinci.model.PendakiFormulir
import com.dicoding.gunungkerinci.model.SimpanFormulirRequest
import com.dicoding.gunungkerinci.model.FormulirItem

class TiketFormDataPendakiActivity : AppCompatActivity() {

    private var bookingId = ""

    private var nomorPendaki = 1

    private var modeEdit = false

    private var totalPendaki = 1

    private lateinit var binding: ActivityTiketFormDataPendakiBinding

    private val dataFormPendaki = mutableListOf<DataFormPendakiSementara>()

    private val pref by lazy {
        getSharedPreferences("booking_formulir", MODE_PRIVATE)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTiketFormDataPendakiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingId = intent.getStringExtra("booking_id") ?: ""

        nomorPendaki = intent.getIntExtra("nomor_pendaki", 1)
        totalPendaki = intent.getIntExtra("total_pendaki", 1)

        modeEdit = intent.getBooleanExtra("mode_edit", false)

        Log.d(
            "FORM_PENDAKI",
            "bookingId=$bookingId | nomorPendaki=$nomorPendaki | modeEdit=$modeEdit"
        )

        repeat(totalPendaki) {
            dataFormPendaki.add(DataFormPendakiSementara())
        }

        if (modeEdit) {
            loadDataPendakiUntukEdit()
        } else {
            tampilkanPendaki()
        }

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

    private fun formulirSudahDisimpan(): Boolean {
        return pref.getBoolean("saved_$bookingId", false)
    }

    private fun loadDataPendakiUntukEdit() {
        lifecycleScope.launch {
            try {
                val response = ApiConfig
                    .getApiService(this@TiketFormDataPendakiActivity)
                    .getDataFormulir(bookingId)

                if (!response.isSuccessful) {
                    Toast.makeText(
                        this@TiketFormDataPendakiActivity,
                        "Gagal mengambil data formulir",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val body = response.body()
                if (body?.success == true) {
                    val daftarPendaki = body.data.booking.pendakis

                    Log.d(
                        "EDIT_PENDAKI",
                        "========================================"
                    )

                    Log.d(
                        "EDIT_PENDAKI",
                        "Nomor pendaki yang dibuka: $nomorPendaki"
                    )

                    Log.d(
                        "EDIT_PENDAKI",
                        "Jumlah pendaki dari backend: ${daftarPendaki.size}"
                    )

                    daftarPendaki.forEachIndexed { index, pendaki ->

                        Log.d(
                            "EDIT_PENDAKI",
                            "INDEX=$index | " +
                                    "id=${pendaki.id} | " +
                                    "id_bio=${pendaki.id_bio} | " +
                                    "nama=${pendaki.biodata.first_name} ${pendaki.biodata.last_name} | " +
                                    "darurat=${pendaki.biodata.no_hp_darurat}"
                        )

                        //Jangan melebihi jumlah slot booking
                        if (index !in dataFormPendaki.indices) {
                            return@forEachIndexed
                        }

                        val biodata = pendaki.biodata

                        dataFormPendaki[index] =
                            DataFormPendakiSementara(
                                idPendaki = pendaki.id,
                                kodeBio = pendaki.id_bio,
                                idPendakiInput = pendaki.id_bio,
                                namaDepan = biodata.first_name.orEmpty(),
                                namaBelakang = biodata.last_name.orEmpty(),
                                negara = biodata.dataNegara?.name.orEmpty(),
                                tanggalLahir = biodata.tanggal_lahir?.substringBefore("T").orEmpty(),
                                usia = pendaki.usia.toString(),
                                noTelepon = biodata.no_hp.orEmpty(),
                                noHpDarurat = if (formulirSudahDisimpan()) {
                                    biodata.no_hp_darurat.orEmpty()
                                } else {
                                    ""
                                },
                                kodeNegara = biodata.dataNegara?.code ?: "ID"
                            )
                    }

                    Log.d(
                        "EDIT_PENDAKI",
                        "========================================"
                    )

                    //Setelah semua data selesai dimasukkan ke slot, tampilkan pendaki
                    //sesuai card Ubah Data yang di klik
                    tampilkanPendaki()
                } else {
                    Toast.makeText(
                        this@TiketFormDataPendakiActivity,
                        body?.message ?: "Data pendaki tidak ditemukan",
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

    private fun validasiSemuaPendaki(): Boolean {

        dataFormPendaki.forEachIndexed { index, data ->
            val nomor = index + 1

            Log.d(
                "VALIDASI_SEMUA_PENDAKI",
                "Pendaki $nomor | " +
                        "id=${data.idPendaki} | " +
                        "kodeBio=${data.kodeBio} | " +
                        "idInput=${data.idPendakiInput} | " +
                        "darurat=${data.noHpDarurat}"
            )

            // =====================================
            // 1. ID Pendaki belum berhasil ditemukan
            // =====================================
            if (data.idPendaki.isBlank() ||
                data.kodeBio.isBlank()
            ) {
                Toast.makeText(
                    this,
                    "Data Pendaki $nomor belum dilengkapi",
                    Toast.LENGTH_SHORT
                ).show()
                nomorPendaki = nomor
                tampilkanPendaki()
                return false
            }

            // =====================================
            // 2. ID input berbeda dengan ID hasil pencarian
            // =====================================
            if (data.idPendakiInput != data.kodeBio) {

                Toast.makeText(
                    this,
                    "ID Pendaki $nomor belum dikonfirmasi. Klik Cari terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()

                nomorPendaki = nomor
                tampilkanPendaki()

                binding.inputIdPendaki.error =
                    "Klik Cari terlebih dahulu"

                binding.inputIdPendaki.requestFocus()
                return false
            }

            // =====================================
            // 3. Nomor darurat wajib
            // =====================================
            if (data.noHpDarurat.isBlank()) {

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

        Log.d(
            "CARI_PENDAKI",
            "Cari untuk Pendaki $nomorPendaki | " +
                    "code=$code | " +
                    "idLama=${dataSaatIni.idPendaki} | " +
                    "kodeBioLama=${dataSaatIni.kodeBio}"
        )

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
                val request = TambahPendakiRequest(
                    booking = bookingId,
                    code = codeBaru,
                    id = idPendaki
                )
                val response = ApiConfig
                    .getApiService(this@TiketFormDataPendakiActivity)
                    .tambahPendaki(request)

                if (response.isSuccessful) {
                    val body = response.body()

                    if (body?.success == true) {
                        Toast.makeText(
                            this@TiketFormDataPendakiActivity,
                            body.message ?: "Pendaki berhasil diperbarui",
                            Toast.LENGTH_SHORT
                        ).show()

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
                        "Gagal memperbarui pendaki. HTTP ${response.code()}",
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
        val index = nomorPendaki - 1

        if (index !in dataFormPendaki.indices) {
            return false
        }

        val data = dataFormPendaki[index]

        // ================================
        // 1. ID Pendaki wajib diisi
        // ================================
        val idPendakiInput =
            binding.inputIdPendaki.text.toString().trim()

        if (idPendakiInput.isEmpty()) {
            binding.inputIdPendaki.error =
                "ID Pendaki wajib diisi"
            binding.inputIdPendaki.requestFocus()
            return false
        }

        // ================================
        // 2. Pastikan ID sudah berhasil dicari
        // ================================
        if (data.idPendaki.isEmpty() || data.kodeBio.isEmpty()) {
            binding.inputIdPendaki.error =
                "Klik Cari terlebih dahulu"
            binding.inputIdPendaki.requestFocus()
            return false
        }

        // ================================
        // 3. Pastikan ID input sama dengan
        //    ID yang berhasil ditemukan
        // ================================
        if (idPendakiInput != data.kodeBio) {
            binding.inputIdPendaki.error =
                "ID Pendaki berubah. Klik Cari terlebih dahulu"
            binding.inputIdPendaki.requestFocus()
            return false
        }

        binding.inputIdPendaki.error = null

        // ================================
        // 4. Nomor darurat wajib
        // ================================
        val noHpDarurat =
            binding.inputTelDarurat.text.toString().trim()

        if (noHpDarurat.isEmpty()) {
            binding.inputTelDarurat.error =
                "Nomor telepon darurat wajib diisi"
            binding.inputTelDarurat.requestFocus()
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
                        pref.edit()
                            .putBoolean("saved_$bookingId", true)
                            .apply()

                        Toast.makeText(
                            this@TiketFormDataPendakiActivity,
                            body.message ?: "Data pendaki berhasil disimpan",
                            Toast.LENGTH_SHORT
                        ).show()

                        val resultIntent = Intent().apply {
                            putExtra("nomor_pendaki", nomorPendaki)
                        }

                        // Kembali ke halaman Data Pendaki
                        setResult(RESULT_OK, resultIntent)
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

        val dataLama = dataFormPendaki[index]

        dataFormPendaki[index] = dataLama.copy(
            idPendaki = dataLama.idPendaki,
            kodeBio = dataLama.kodeBio,

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
                binding.inputTelDarurat.text.toString().trim(),

            kodeNegara = binding.ccpDarurat.selectedCountryNameCode
        )

        Log.d(
            "SIMPAN_FORM_SEMENTARA",
            "Pendaki $nomorPendaki | " +
                    "idPendaki=${dataFormPendaki[index].idPendaki} | " +
                    "kodeBio=${dataFormPendaki[index].kodeBio} | " +
                    "idInput=${dataFormPendaki[index].idPendakiInput} | " +
                    "darurat=${dataFormPendaki[index].noHpDarurat}"
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
        binding.ccpDarurat.setCountryForNameCode(data.kodeNegara)

        Log.d(
            "CCP_DARURAT",
            "Tampilkan Pendaki $nomorPendaki | kodeNegara=${data.kodeNegara} | " +
                    "prefix=${binding.ccpDarurat.selectedCountryCodeWithPlus}"
        )

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
        val index = nomorPendaki - 1

        if (index !in dataFormPendaki.indices) {
            return
        }

        val biodata = pendaki.biodata

        Log.d(
            "CEK_DARURAT_CARI",
            "SEBELUM isiDataPendaki | " +
                    "Pendaki=$nomorPendaki | " +
                    "modeEdit=$modeEdit | " +
                    "daruratBackend=${biodata.no_hp_darurat}"
        )

        // =====================================
        // Simpan hasil pencarian ke slot aktif
        // =====================================
        dataFormPendaki[index] = DataFormPendakiSementara(
            idPendaki = pendaki.id,
            kodeBio = pendaki.id_bio,
            idPendakiInput = pendaki.id_bio,
            namaDepan = biodata.first_name.orEmpty(),
            namaBelakang = biodata.last_name.orEmpty(),
            negara = biodata.dataNegara?.name.orEmpty(),
            tanggalLahir = biodata.tanggal_lahir?.substringBefore("T").orEmpty(),
            usia = pendaki.usia.toString(),
            noTelepon = biodata.no_hp.orEmpty(),
            noHpDarurat =
                if (
                    dataFormPendaki[index].kodeBio == pendaki.id_bio
                ) {
                    dataFormPendaki[index].noHpDarurat
                } else {
                    ""
                },
            kodeNegara = biodata.dataNegara?.code ?: "ID"
        )

        Log.d(
            "CEK_DARURAT_CARI",
            "SESUDAH isiDataPendaki | " +
                    "Pendaki=$nomorPendaki | " +
                    "daruratSementara=${dataFormPendaki[index].noHpDarurat}"
        )

        // =====================================
        // Tampilkan ke UI
        // =====================================
        tampilkanPendaki()

        Log.d(
            "CEK_DARURAT_CARI",
            "SETELAH tampilkanPendaki | " +
                    "Pendaki=$nomorPendaki | " +
                    "daruratEditText=${binding.inputTelDarurat.text}"
        )

        binding.inputTelDarurat.error = null

        Log.d(
            "HASIL_CARI_PENDAKI",
            "Pendaki $nomorPendaki berhasil ditemukan | " +
                    "id=${pendaki.id} | " +
                    "kodeBio=${pendaki.id_bio} | " +
                    "darurat=${biodata.no_hp_darurat}"
        )
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
    var noHpDarurat: String = "",
    var kodeNegara: String = "ID"
)