package com.dicoding.gunungkerinci.Homepage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.dicoding.gunungkerinci.Homepage.Panduan.PanduanActivity
import com.dicoding.gunungkerinci.Homepage.Pemberitahuan.PemberitahuanActivity
import com.dicoding.gunungkerinci.Homepage.Sop.SopActivity
import com.dicoding.gunungkerinci.Homepage.Wisata.WisataActivity
import com.dicoding.gunungkerinci.R
import com.dicoding.gunungkerinci.Ticket.PilihTiketActivity
import com.dicoding.gunungkerinci.Ticket.Barcode.BarcodeTiketActivity
import com.dicoding.gunungkerinci.databinding.FragmentHomeBinding
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.pref.UserPreference
import android.os.Build
import android.text.Html
import com.dicoding.gunungkerinci.Homepage.Cuaca.CuacaActivity
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.ActiveTicketCard
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.TicketUiMapper
import com.bumptech.glide.Glide
import androidx.navigation.fragment.findNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dicoding.gunungkerinci.data.local.TnksDatabase
import com.dicoding.gunungkerinci.data.repository.PeringatanDiniRepository
import com.dicoding.gunungkerinci.data.repository.TrackingGpsRepository
import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDiniOverlay
import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDiniViewModel
import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDiniViewModelFactory
import com.dicoding.gunungkerinci.util.LokasiProvider
import com.dicoding.gunungkerinci.util.SessionOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val peringatanDiniViewModel: PeringatanDiniViewModel by viewModels {
        PeringatanDiniViewModelFactory(
            PeringatanDiniRepository(
                ApiConfig.getApiService(requireContext()),
                dao = TnksDatabase.getInstance(requireContext()).peringatanDiniDao()
            ),
            TrackingGpsRepository(ApiConfig.getApiService(requireContext()))
        )
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val ctx = context ?: return@registerForActivityResult
        if (results.values.any { it }) {
            doUpdateLastLocation()
        } else {
            Toast.makeText(ctx, "Izin lokasi ditolak. Berikan izin lokasi untuk memperbarui posisi.", Toast.LENGTH_LONG).show()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupMenu()

        setupPeringatanDiniOverlay()

        getDestinasi()

        setupBeranda()

        setupTiketAktif()

        //setupWisataList()

        return binding.root
    }

    // Peringatan dini: overlay Compose + polling selama layar terlihat
    private fun setupPeringatanDiniOverlay() {
        binding.composePeringatan.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by peringatanDiniViewModel.uiState.collectAsState()
                PeringatanDiniOverlay(
                    uiState = uiState,
                    onBubbleClick = peringatanDiniViewModel::openPopup,
                    onClosePopup = peringatanDiniViewModel::dismissPopup,
                    onUpdateLocation = { onUpdateLocationClick() }
                )
            }
        }
    }

    // Update lokasi manual dari tombol di popup peringatan dini
    private fun onUpdateLocationClick() {
        val ctx = requireContext()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            doUpdateLastLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Minta lokasi baru dari GPS, bukan cuma baca catatan lama (lihat LokasiProvider)
    private fun doUpdateLastLocation() {
        val ctx = requireContext()
        if (!LokasiProvider.gpsAktif(ctx)) {
            Toast.makeText(ctx, "GPS belum aktif. Nyalakan GPS lalu coba lagi.", Toast.LENGTH_LONG).show()
            return
        }

        peringatanDiniViewModel.mulaiCariLokasi()
        LokasiProvider.cariSekarang(ctx) { lokasi ->
            // View bisa sudah hancur saat GPS menjawab.
            if (!isAdded) return@cariSekarang
            if (lokasi == null) {
                peringatanDiniViewModel.gagalCariLokasi("Sinyal lokasi belum dapat. Coba lagi sebentar.")
                return@cariSekarang
            }
            peringatanDiniViewModel.updateLastLocation(
                token = UserPreference(ctx).getToken().orEmpty(),
                latitude = lokasi.latitude,
                longitude = lokasi.longitude,
                altitude = if (lokasi.hasAltitude()) lokasi.altitude else null,
                accuracy = if (lokasi.hasAccuracy()) lokasi.accuracy.toDouble() else null
            )
        }
    }

    private fun startWarningPolling() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    val token = UserPreference(requireContext()).getToken().orEmpty()
                    peringatanDiniViewModel.load(token, SessionOwner.currentKey(requireContext()))
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startWarningPolling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val POLL_INTERVAL_MS = 15_000L
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
                                "https://tnks.satriabahari.my.id/${destinasi.gambar_destinasi[0].src}"
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

                    // Label status gunung (Gunung Kerinci) dari backend.
                    result?.data?.firstOrNull { it.id == 1 }?.let { kerinci ->
                        binding.statusGunung.text = kerinci.status_gunung_label
                    }
                }
            } catch (e: Exception) {
                Log.d("DESTINASI_API", e.message.toString())
            }
        }
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
        // Menu Berkas (6 item) sekarang Compose — sama seperti repo vibe-coding
        binding.composeViewBerkasMenu.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                BerkasMenuGrid(
                    onPesanClick      = { startActivity(Intent(requireContext(), SopActivity::class.java)) },
                    onVrJalurClick    = { startActivity(Intent(requireContext(), SopActivity::class.java)) },
                    onSopClick        = { startActivity(Intent(requireContext(), SopActivity::class.java)) },
                    onLaporanClick    = { startActivity(Intent(requireContext(), SopActivity::class.java)) },
                    onSertifikatClick = { startActivity(Intent(requireContext(), SopActivity::class.java)) },
                    onPanduanClick    = { startActivity(Intent(requireContext(), PanduanActivity::class.java)) }
                )
            }
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

    /**
     * Kartu cuaca + jumlah pendaki dari `GET /api/beranda`.
     * `cuaca` bisa null (WeatherAPI di backend gagal) -> kolom diisi "-".
     */
    private fun setupBeranda() {
        val token = UserPreference(requireContext()).getToken().orEmpty()

        binding.loadingCuaca.isVisible = true
        binding.grupKontenCuaca.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(requireContext())
                    .getBeranda("Bearer $token")

                val data = response.body()?.data

                val cuaca = data?.cuaca
                val current = cuaca?.current
                val condition = current?.condition

                binding.suhu.text = current?.tempC?.let { "${it.toInt()}°c" } ?: "-"
                binding.ketSuhu.text = condition?.text ?: "-"
                binding.ketKota.text = cuaca?.location?.name ?: "-"

                condition?.icon?.let { icon ->
                    Glide.with(this@HomeFragment)
                        .load("https:$icon")
                        .placeholder(R.drawable.ket_status)
                        .into(binding.iconStatus)
                }

                binding.pendakiNow.text = (data?.totalMendaki ?: 0).toString()
                binding.JmlpendakiNow.text = (data?.totalPendaki ?: 0).toString()
            } catch (e: Exception) {
                binding.suhu.text = "-"
                binding.ketSuhu.text = "-"
                binding.ketKota.text = "-"
                Log.d("BERANDA_API", e.message.toString())
            } finally {
                binding.loadingCuaca.isVisible = false
                binding.grupKontenCuaca.isVisible = true
            }
        }
    }

    /**
     * Kartu "Tiket Anda Saat Ini" — pakai ulang kartu layar Pelacakan Jejak.
     * Hanya booking status 6 (sedang mendaki) yang tampil.
     */
    private fun setupTiketAktif() {
        val token = UserPreference(requireContext()).getToken().orEmpty()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(requireContext())
                    .getMyTiket("Bearer $token")

                val bookings = response.body()?.data.orEmpty()
                val activeTicket = TicketUiMapper.toActiveTicket(bookings) ?: return@launch

                binding.layoutBelumAda.visibility = View.GONE
                binding.composeViewTiketAktif.visibility = View.VISIBLE
                binding.composeViewTiketAktif.setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                binding.composeViewTiketAktif.setContent {
                    ActiveTicketCard(
                        ticket = activeTicket,
                        badgeText = "Lihat Tiket →",
                        onBadgeClick = {
                            startActivity(
                                Intent(requireContext(), BarcodeTiketActivity::class.java)
                            )
                        },
                        onCheckPointClick = {
                            findNavController().navigate(R.id.navigation_jejak)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.d("MYTIKET_API", e.message.toString())
            }
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