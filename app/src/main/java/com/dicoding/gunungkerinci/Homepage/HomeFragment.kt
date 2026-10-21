package com.dicoding.gunungkerinci.Homepage

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.dicoding.gunungkerinci.ui.peringatan_dini.WarningMonitoring
import com.dicoding.gunungkerinci.ui.peringatan_dini.WarningNotifications
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
import kotlinx.coroutines.CancellationException
import com.dicoding.gunungkerinci.network.ApiConfig
import com.dicoding.gunungkerinci.pref.UserPreference
import android.os.Build
import android.text.Html
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.ActiveTicketCard
import com.dicoding.gunungkerinci.ui.pelacakan_jejak.TicketUiMapper
import androidx.appcompat.app.AlertDialog
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import com.dicoding.gunungkerinci.model.BerandaData
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
    private var berandaJob: Job? = null
    private var greetingJob: Job? = null
    private var cardData: BerandaData? = null

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
                val alarmRinging by WarningMonitoring.ringing.collectAsState()
                PeringatanDiniOverlay(
                    uiState = uiState,
                    onBubbleClick = peringatanDiniViewModel::openPopup,
                    onClosePopup = peringatanDiniViewModel::dismissPopup,
                    onUpdateLocation = { onUpdateLocationClick() },
                    alarmRinging = alarmRinging,
                    onStopAlarm = { WarningMonitoring.stopAlarm(requireContext()) }
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

        val requestBinding = _binding ?: return
        if (!peringatanDiniViewModel.mulaiCariLokasi()) return
        LokasiProvider.cariSekarang(ctx) { lokasi ->
            // View bisa sudah hancur saat GPS menjawab.
            if (!isAdded || _binding !== requestBinding) return@cariSekarang
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
        val warningHandle = findNavController().getBackStackEntry(R.id.navigation_beranda).savedStateHandle
        warningHandle.getLiveData<Boolean>(WarningNotifications.OPEN_WARNING).observe(viewLifecycleOwner) { shouldOpen ->
                if (shouldOpen == true) {
                    peringatanDiniViewModel.openNotificationWarning(WarningMonitoring.alert.value)
                    warningHandle[WarningNotifications.OPEN_WARNING] = false
                }
            }
        startWarningPolling()
    }

    override fun onResume() {
        super.onResume()
        loadGreeting()
    }

    private fun loadGreeting() {
        val requestBinding = _binding ?: return
        val token = UserPreference(requireContext()).getToken().orEmpty()
        if (token.isBlank() || greetingJob?.isActive == true) return
        greetingJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiConfig.getApiService(requireContext()).getProfile("Bearer $token")
                if (_binding !== requestBinding || !response.isSuccessful) return@launch
                val profile = response.body()?.data ?: return@launch
                val name = listOfNotNull(profile.first_name, profile.last_name)
                    .map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
                if (name.isNotEmpty()) {
                    requestBinding.halo.text = "Halo,"
                    requestBinding.User.text = name
                } else {
                    requestBinding.halo.text = "Halo!"
                    requestBinding.User.text = ""
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Pertahankan sapaan terakhir saat profil belum dapat dimuat.
            }
        }
    }

    override fun onDestroyView() {
        if (peringatanDiniViewModel.uiState.value.isUpdatingLocation && !peringatanDiniViewModel.uiState.value.isSendingLocation) {
            peringatanDiniViewModel.gagalCariLokasi("Pencarian lokasi dibatalkan. Coba lagi.")
        }
        berandaJob?.cancel()
        berandaJob = null
        greetingJob?.cancel()
        greetingJob = null
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
                val binding = _binding ?: return@launch
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


                }
            } catch (e: CancellationException) {
                throw e
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
            showHomeCardDetails()
        }

    }

    /**
     * Kartu cuaca + jumlah pendaki dari `GET /api/beranda`.
     * `cuaca` bisa null (WeatherAPI di backend gagal) -> kolom diisi "-".
     */
    private fun setupBeranda() {
        val requestBinding = _binding ?: return
        if (berandaJob?.isActive == true) return
        cardData?.let { renderHomeCard(it) }
        requestBinding.loadingCuaca.isVisible = true
        requestBinding.weatherNoticeRow.isVisible = true
        requestBinding.weatherNotice.text = if (cardData == null) "Memuat informasi..." else "Memperbarui..."
        requestBinding.cardCuaca.isClickable = true
        requestBinding.cardCuaca.isFocusable = true
        berandaJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = UserPreference(requireContext()).getToken().orEmpty()
                val response = ApiConfig.getApiService(requireContext()).getBeranda("Bearer $token")
                val body = response.body()
                if (!response.isSuccessful || body?.success != true || body.data == null) {
                    throw IllegalStateException("Data beranda belum tersedia")
                }
                if (_binding !== requestBinding) return@launch
                cardData = body.data
                renderHomeCard(body.data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_binding !== requestBinding) return@launch
                renderHomeCard(cardData)
            } finally {
                if (_binding === requestBinding) requestBinding.loadingCuaca.isVisible = false
            }
        }
    }

    private fun renderHomeCard(data: BerandaData?) {
        val binding = _binding ?: return
        val weather = data?.cuaca
        val current = weather?.current
        val active = data?.ringkasanPendaki?.sedangMendaki ?: data?.totalMendaki
        val total = data?.ringkasanPendaki?.totalTercatat ?: data?.totalPendaki
        binding.grupKontenCuaca.isVisible = true
        binding.suhu.text = current?.tempC?.let { "${it.toInt()}°C" } ?: "—"
        binding.ketSuhu.text = current?.condition?.text?.takeIf { it.isNotBlank() } ?: "Belum tersedia"
        binding.ketKota.text = weather?.location?.name?.takeIf { it.isNotBlank() } ?: "Gunung Kerinci"
        val status = data?.statusGunung?.takeIf { it.kode in 0..4 }
        binding.statusGunung.text = status?.label?.takeIf { it.isNotBlank() } ?: "Belum tersedia"
        binding.statusGunung.setTextColor(android.graphics.Color.parseColor(when (status?.kode) {
            0, 1 -> "#166534"
            2 -> "#92400E"
            3, 4 -> "#B42318"
            else -> "#475467"
        }))
        binding.iconStatus.isVisible = status?.kode in 2..4
        val first = active?.toString() ?: "—"
        val counts = "$first/${total ?: "—"} Total Pendaki"
        binding.pendakiNow.text = SpannableString(counts).apply {
            setSpan(RelativeSizeSpan(12f / 18f), first.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(StyleSpan(Typeface.NORMAL), first.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(TypefaceSpan("sans-serif"), first.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        binding.pendakiNow.contentDescription = "${active ?: "Belum diketahui"} sedang mendaki; ${total ?: "belum diketahui"} total kunjungan tercatat. Bukan kuota."
        binding.weatherNoticeRow.isVisible = false
    }

    private fun showHomeCardDetails() {
        val data = cardData
        val active = data?.ringkasanPendaki?.sedangMendaki ?: data?.totalMendaki
        val total = data?.ringkasanPendaki?.totalTercatat ?: data?.totalPendaki
        val observed = data?.cuacaDiamatiPada?.let {
            SimpleDateFormat("dd MMM yyyy HH.mm 'WIB'", Locale.forLanguageTag("id-ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }.format(Date(it * 1000))
        } ?: "Belum tersedia"
        val summaryTime = com.dicoding.gunungkerinci.util.DateTimeFormat
            .apiWibLongDateTime(data?.diperbaruiPada) ?: "Belum tersedia"
        AlertDialog.Builder(requireContext())
            .setTitle("Cuaca dan pendaki")
            .setMessage(buildString {
                append("Cuaca: ${data?.cuaca?.current?.tempC?.let { "$it°C" } ?: "Belum tersedia"}")
                append("\n${data?.cuaca?.current?.condition?.text ?: "Kondisi belum tersedia"}")
                append("\nPengamatan cuaca: $observed")
                append("\n\nStatus gunung: ${data?.statusGunung?.label ?: "Belum tersedia"}")
                append("\nTingkat aktivitas gunung dikelola petugas; berbeda dari kondisi cuaca.")
                append("\n\n${active ?: "—"} sedang mendaki dari ${total ?: "—"} total kunjungan tercatat.")
                append("\nTotal mencakup seluruh riwayat kunjungan yang sudah check-in, termasuk yang sudah turun. Bukan kuota atau total hari ini.")
                append("\nRingkasan diperbarui: $summaryTime")
            })
            .setNeutralButton("Perbarui") { _, _ -> setupBeranda() }
            .setPositiveButton("Tutup", null)
            .show()
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
                val binding = _binding ?: return@launch

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
                            val intent = Intent(requireContext(), BarcodeTiketActivity::class.java)
                            activeTicket.rawBookingId?.let { intent.putExtra("booking_id", it) }
                            startActivity(intent)
                        },
                        onCheckPointClick = {
                            findNavController().navigate(R.id.navigation_jejak)
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
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
