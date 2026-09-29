package com.dicoding.gunungkerinci.Ticket.DataPendaki

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.Ticket.RincianPemesananTiketActivity
import com.dicoding.gunungkerinci.databinding.ActivityDetailDataPendakiBinding
import com.dicoding.gunungkerinci.model.BiodataPendakiFormulir
import com.dicoding.gunungkerinci.model.BookingDetailPendaki
import com.dicoding.gunungkerinci.network.ApiConfig
import kotlinx.coroutines.launch

class DetailDataPendakiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailDataPendakiBinding

    private var bookingId = ""
    private var ketuaPendakiId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityDetailDataPendakiBinding.inflate(layoutInflater)

        setContentView(binding.root)

        bookingId =
            intent.getStringExtra("booking_id").orEmpty()

        ketuaPendakiId =
            intent.getStringExtra("ketua_pendaki_id").orEmpty()

        if (bookingId.isBlank()) {

            Toast.makeText(
                this,
                "Booking ID tidak ditemukan",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        binding.buttonBack.setOnClickListener {

            val intent = Intent(
                this,
                RincianPemesananTiketActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)

            finish()
        }

        binding.recyleViewDataPendaki.layoutManager =
            LinearLayoutManager(this)

        ambilDataPendaki()
    }

    private fun ambilDataPendaki() {

        lifecycleScope.launch {

            try {

                val response =
                    ApiConfig
                        .getApiService(
                            this@DetailDataPendakiActivity
                        )
                        .getBookingDetail(bookingId)

                Log.d(
                    "DETAIL_PENDAKI_API",
                    "HTTP Code: ${response.code()}"
                )

                if (!response.isSuccessful) {

                    Toast.makeText(
                        this@DetailDataPendakiActivity,
                        "Gagal mengambil data pendaki",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val body = response.body()

                Log.d(
                    "DETAIL_PENDAKI_API",
                    "Response: $body"
                )

                if (body?.success != true) {

                    Toast.makeText(
                        this@DetailDataPendakiActivity,
                        body?.message
                            ?: "Data pendaki tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                Log.d(
                    "DETAIL_PENDAKI_DATA",
                    "Jumlah pendaki: ${body.data.pendakis.size}"
                )

                Log.d(
                    "DETAIL_PENDAKI_DATA",
                    "Ketua ID: $ketuaPendakiId"
                )

                tampilkanDataPendaki(
                    body.data.pendakis
                )

            } catch (e: Exception) {

                Log.e(
                    "DETAIL_PENDAKI_API",
                    "Error: ${e.message}",
                    e
                )

                Toast.makeText(
                    this@DetailDataPendakiActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun tampilkanDataPendaki(
        dataPendaki: List<BookingDetailPendaki>
    ) {

        val listPendaki =
            dataPendaki.map { pendaki ->

                val biodata =
                    pendaki.biodata

                val nama =
                    listOfNotNull(
                        biodata.first_name,
                        biodata.last_name
                    )
                        .joinToString(" ")
                        .trim()

                val kewarganegaraan =
                    if (
                        biodata.dataNegara?.code
                            .equals("ID", ignoreCase = true)
                    ) {
                        "WNI"
                    } else {
                        "WNA"
                    }

                val jenisKelamin =
                    when (
                        biodata.jenis_kelamin
                            ?.lowercase()
                    ) {
                        "l" -> "Pria"
                        "p" -> "Perempuan"
                        else -> "-"
                    }

                val tanggalLahir =
                    formatTanggalLahir(
                        biodata.tanggal_lahir
                    )

                val alamat =
                    buatAlamat(biodata)

                val status =
                    if (
                        pendaki.id == ketuaPendakiId
                    ) {
                        "Pemandu"
                    } else {
                        ""
                    }

                Pendaki(
                    nama = nama.ifBlank { "-" },

                    idPendaki =
                        pendaki.id_bio,

                    kewarganegaraan =
                        kewarganegaraan,

                    noIdentitas =
                        biodata.nik.orEmpty()
                            .ifBlank { "-" },

                    jenisKelamin =
                        jenisKelamin,

                    tanggalLahir =
                        tanggalLahir,

                    alamat =
                        alamat,

                    noTelepon =
                        biodata.no_hp.orEmpty()
                            .ifBlank { "-" },

                    noDarurat =
                        biodata.no_hp_darurat.orEmpty()
                            .ifBlank { "-" },

                    status =
                        status
                )
            }

        binding.recyleViewDataPendaki.adapter =
            DetailPendakiAdapter(listPendaki)
    }

    private fun formatTanggalLahir(
        tanggal: String?
    ): String {

        if (tanggal.isNullOrBlank()) {
            return "-"
        }

        return try {

            val input =
                java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                    java.util.Locale.getDefault()
                )

            input.timeZone =
                java.util.TimeZone.getTimeZone("UTC")

            val output =
                java.text.SimpleDateFormat(
                    "dd - MM - yyyy",
                    java.util.Locale.getDefault()
                )

            output.format(
                input.parse(tanggal)!!
            )

        } catch (e: Exception) {

            tanggal
                .substringBefore("T")
                .replace("-", " - ")
        }
    }

    private fun buatAlamat(
        biodata: com.dicoding.gunungkerinci.model.BookingDetailBiodata
    ): String {

        val bagianAlamat =
            listOfNotNull(
                biodata.dataDesa?.name,
                biodata.dataKecamatan?.name,
                biodata.dataKabupaten?.name,
                biodata.dataProvinsi?.name,
                biodata.dataNegara?.name
            )
                .filter {
                    it.isNotBlank()
                }

        return if (bagianAlamat.isNotEmpty()) {

            bagianAlamat.joinToString(", ")

        } else {

            "-"
        }
    }
}