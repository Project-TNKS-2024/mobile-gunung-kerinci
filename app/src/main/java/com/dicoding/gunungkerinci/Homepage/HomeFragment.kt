package com.dicoding.gunungkerinci.Homepage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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
import com.dicoding.gunungkerinci.databinding.FragmentHomeBinding
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.dicoding.gunungkerinci.network.ApiConfig
import android.os.Build
import android.text.Html
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.dicoding.gunungkerinci.data.repository.EmergencyRepository
import com.dicoding.gunungkerinci.data.repository.TrackingGpsRepository
import com.dicoding.gunungkerinci.pref.UserPreference
import com.dicoding.gunungkerinci.ui.peringatan_dini.EarlyWarningOverlay
import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDiniViewModel
import com.dicoding.gunungkerinci.ui.peringatan_dini.PeringatanDiniViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private val peringatanDiniViewModel: PeringatanDiniViewModel by viewModels {
        PeringatanDiniViewModelFactory(
            EmergencyRepository(ApiConfig.getApiService(requireContext()))
            EmergencyRepository(ApiConfig.getApiService(requireContext())),
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
            Toast.makeText(ctx, "Izin lokasi ditolak. Aktifkan GPS untuk memperbarui lokasi.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupMenu()

        setupPeringatanDiniOverlay()

        getDestinasi()

        //setupWisataList()

        return binding.root
    }

    // Peringatan dini: overlay Compose + polling selagi layar terlihat.
    private fun setupPeringatanDiniOverlay() {
        binding.composePeringatan.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by peringatanDiniViewModel.uiState.collectAsState()
                EarlyWarningOverlay(
                    uiState = uiState,
                    onBubbleClick = peringatanDiniViewModel::openPopup,
                    onClosePopup = peringatanDiniViewModel::dismissPopup
                    onClosePopup = peringatanDiniViewModel::dismissPopup,
                    onUpdateLocation = { onUpdateLocationClick() }
                )
            }
        }
    }

    // Jalur manual pembaruan lokasi dari popup peringatan dini (tombol desain UI/UX).
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

    private fun doUpdateLastLocation() {
        val location = getLastKnownLocation()
        if (location == null) {
            Toast.makeText(requireContext(), "Lokasi tidak tersedia. Aktifkan GPS lalu coba lagi.", Toast.LENGTH_LONG).show()
            return
        }
        peringatanDiniViewModel.updateLastLocation(
            token = UserPreference(requireContext()).getToken().orEmpty(),
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy.toDouble()
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun startWarningPolling() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    val token = UserPreference(requireContext()).getToken().orEmpty()
                    peringatanDiniViewModel.load(token)
                    delay(POLL_INTERVAL_MS)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startWarningPolling()
    }

    private companion object {
        const val POLL_INTERVAL_MS = 60_000L
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

    }

    private fun htmlToText(html: String): String {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            Html.fromHtml(html).toString()
        }

    }
}