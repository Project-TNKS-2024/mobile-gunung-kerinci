package com.dicoding.gunungkerinci.Homepage

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.dicoding.gunungkerinci.Homepage.Panduan.PanduanActivity
import com.dicoding.gunungkerinci.Homepage.Pemberitahuan.PemberitahuanActivity
import com.dicoding.gunungkerinci.Homepage.Sop.SopActivity
import com.dicoding.gunungkerinci.Homepage.Wisata.WisataActivity
import com.dicoding.gunungkerinci.Ticket.PilihTiketActivity
import com.dicoding.gunungkerinci.databinding.FragmentHomeBinding
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.dicoding.gunungkerinci.network.ApiConfig
import android.os.Build
import android.text.Html
import com.dicoding.gunungkerinci.Homepage.Cuaca.CuacaActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.model.BookingDetailData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupMenu()

        getDestinasi()

        getTiketAktif()

        return binding.root
    }

    private fun getDestinasi() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(requireContext())
                    .getDestinasi()
                if (response.isSuccessful) {

                    val result = response.body()

                    val listWisata = mutableListOf<WisataItem>()

                    result?.data?.forEach { destinasi ->

                        val imageUrl =
                            if (destinasi.gambar_destinasi.isNotEmpty()) {
                                "https://eticket-tnks.fst.unja.ac.id/${destinasi.gambar_destinasi[0].src}"
                            } else {
                                ""
                            }

                        val plainText =
                            htmlToText(destinasi.detail)

                        val shortDesc =
                            if (plainText.length > 80)
                                plainText.substring(0, 80) + "..."
                            else
                                plainText

                        listWisata.add(
                            WisataItem(
                                id = destinasi.id,
                                title = destinasi.nama,
                                shortDesc = shortDesc,
                                longDesc = plainText,
                                imageUrl = imageUrl
                            )
                        )
                    }

                    val adapter = WisataAdapter(listWisata)

                    binding.recyclerViewWisata.layoutManager =
                        LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )

                    binding.recyclerViewWisata.adapter = adapter
                }
            } catch (e: Exception) {
                Log.d("DESTINASI_API", e.message.toString())
            }
        }
    }

    private fun getTiketAktif() {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response = ApiConfig
                    .getApiService(requireContext())
                    .getMyTiket()

                Log.d(
                    "HOME_TIKET",
                    "HTTP Code: ${response.code()}"
                )

                if (!response.isSuccessful) {

                    Log.e(
                        "HOME_TIKET",
                        "HTTP Error: ${response.code()}"
                    )

                    tampilkanTidakAdaTiket()
                    return@launch
                }

                val body = response.body()

                if (body?.success != true) {

                    Log.e(
                        "HOME_TIKET",
                        "API gagal: ${body?.message}"
                    )

                    tampilkanTidakAdaTiket()
                    return@launch
                }

                val semuaBooking = body.data ?: emptyList()

                val hariIni = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Date())

                val tiketAktif = semuaBooking.firstOrNull { booking ->

                    val statusPembayaran = booking.pembayaran
                        ?.lastOrNull()
                        ?.status
                        ?.equals("success", ignoreCase = true) == true

                    val tanggalAktif =
                        hariIni >= booking.tanggal_masuk &&
                                hariIni <= booking.tanggal_keluar

                    statusPembayaran && tanggalAktif
                }

                if (tiketAktif != null) {

                    Log.d(
                        "HOME_TIKET",
                        "Tiket aktif ditemukan: ${tiketAktif.id}"
                    )

                    tampilkanTiketAktif(tiketAktif)

                } else {

                    Log.d(
                        "HOME_TIKET",
                        "Tidak ada tiket aktif"
                    )

                    tampilkanTidakAdaTiket()
                }

            } catch (e: Exception) {

                Log.e(
                    "HOME_TIKET",
                    "Exception mengambil tiket aktif",
                    e
                )

                tampilkanTidakAdaTiket()
            }
        }
    }

    private fun tampilkanTiketAktif(
        booking: BookingDetailData
    ) {

        binding.layoutBelumAda.removeAllViews()

        binding.layoutBelumAda.background = null

        val view = layoutInflater.inflate(
            R.layout.item_tiketnow,
            binding.layoutBelumAda,
            false
        )

        val textPembelian =
            view.findViewById<android.widget.TextView>(
                R.id.textPembelian
            )

        val dateBeli =
            view.findViewById<android.widget.TextView>(
                R.id.dateBeli
            )

        val dateMasuk =
            view.findViewById<android.widget.TextView>(
                R.id.dateMasuk
            )

        val dateKeluar =
            view.findViewById<android.widget.TextView>(
                R.id.dateKeluar
            )

        val jumlahPendaki =
            view.findViewById<android.widget.TextView>(
                R.id.jmlhPendaki
            )

        val total =
            view.findViewById<android.widget.TextView>(
                R.id.Total
            )

        textPembelian.text = "Pembelian"

        dateBeli.text =
            formatTanggal(booking.tanggal_masuk)

        dateMasuk.text =
            formatTanggal(booking.tanggal_masuk)

        dateKeluar.text =
            formatTanggal(booking.tanggal_keluar)

        jumlahPendaki.text =
            booking.pendakis.size.toString()

        total.text =
            formatRupiah(booking.total_pembayaran)

        binding.layoutBelumAda.addView(view)
    }

    private fun formatTanggal(
        tanggal: String?
    ): String {

        if (tanggal.isNullOrBlank()) {
            return "-"
        }

        return try {

            val inputFormat =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val outputFormat =
                SimpleDateFormat(
                    "dd MMMM yyyy",
                    Locale("id", "ID")
                )

            val date =
                inputFormat.parse(tanggal)

            outputFormat.format(date!!)

        } catch (e: Exception) {

            tanggal
        }
    }

    private fun formatRupiah(
        nominal: Int
    ): String {

        return java.text.NumberFormat
            .getNumberInstance(Locale("id", "ID"))
            .format(nominal)
    }

    private fun tampilkanTidakAdaTiket() {

        binding.layoutBelumAda.removeAllViews()

        binding.layoutBelumAda.background =
            resources.getDrawable(
                R.drawable.border_ticket,
                null
            )

        val imageView = android.widget.ImageView(requireContext())

        imageView.layoutParams =
            android.widget.LinearLayout.LayoutParams(
                150,
                120
            )

        imageView.setImageResource(
            R.drawable.not_ticket
        )

        val textView =
            android.widget.TextView(requireContext())

        textView.text =
            "Anda belum memiliki tiket aktif"

        textView.setTextColor(
            resources.getColor(
                R.color.grey,
                null
            )
        )

        textView.textSize = 14f
        textView.textAlignment =
            android.view.View.TEXT_ALIGNMENT_CENTER

        binding.layoutBelumAda.addView(imageView)
        binding.layoutBelumAda.addView(textView)
    }

    /*
    private fun setupWisataList() {
        val listWisata = listOf(
            WisataItem(
                "Danau Gunung Tujuh",
                "Danau tertinggi di Asia Tenggara, destinasi favorit pendaki.",
                "Danau Gunung Tujuh adalah danau vulkanik di ketinggian 1.950 mdpl yang dikelilingi tujuh puncak gunung. Panorama alamnya sangat indah dan menjadi favorit pendaki dari berbagai daerah.",
                R.drawable.splash4
            ),
            WisataItem(
                "Gunung Kerinci",
                "Gunung Kerinci adalah gunung berapi tertinggi di Indonesia",
                "Gunung Kerinci adalah gunung berapi tertinggi di Indonesia dengan ketinggian 3.805 meter di atas permukaan laut. Terletak di Provinsi Jambi, gunung ini menjadi bagian dari Taman Nasional Kerinci Seblat.",
                R.drawable.splash1
            )
        )

        val adapter = WisataAdapter(listWisata)

        binding.recyclerViewWisata.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerViewWisata.adapter = adapter
    } */

    private fun setupMenu() {
        binding.cardSOP.setOnClickListener {
            startActivity(Intent(requireContext(), SopActivity::class.java))
        }

        binding.cardPanduan.setOnClickListener {
            startActivity(Intent(requireContext(), PanduanActivity::class.java))
        }

        binding.textViewSelengkapnya.setOnClickListener {
            startActivity(Intent(requireContext(), WisataActivity::class.java))
        }

        // 👉 BUTTON PESAN TIKET
        binding.buttonPesanTiket.setOnClickListener {
            startActivity(Intent(requireContext(), PilihTiketActivity::class.java))
        }

        binding.buttonNotif.setOnClickListener {
            startActivity(Intent(requireContext(), PemberitahuanActivity::class.java))
        }

        binding.cardCuaca.setOnClickListener {
            startActivity(Intent(requireContext(), CuacaActivity::class.java))
        }

    }

    private fun htmlToText(html: String): String {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            Html.fromHtml(html).toString()
        }

    }
}